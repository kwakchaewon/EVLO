# Spring 연동 메뉴얼 — EVTX 파서 서비스

Spring 애플리케이션에서 **EVTX 파서 서비스**(evtx-service)를 연동하는 방법을 정리한 문서입니다.

---

## 1. 개요

### 1.1 EVTX 파서 서비스

- **역할**: Windows 이벤트 로그(.evtx) 파일을 **JSON**으로 변환하는 REST API
- **구현**: Python FastAPI + python-evtx, 기본 포트 **8081**
- **배포**: Docker 컨테이너(evtx-service) 또는 단독 프로세스

### 1.2 연동 방식

| 방식 | 설명 |
|------|------|
| **A. evtx-service 직접 호출** | Spring에서 WebClient 등으로 `POST /parse` 호출 |
| **B. simple-evtx Spring Boot API 경유** | `POST /api/evtx/parse` 등으로 우리쪽 Spring이 evtx-service를 대신 호출 |

본 메뉴얼은 **A(직접 호출)** 기준으로 작성합니다. B는 simple-evtx 프로젝트를 그대로 사용하면 됩니다.

---

## 2. evtx-service API 스펙

### 2.1 Base URL

- Docker Compose: `http://evtx-service:8081`
- 로컬: `http://localhost:8081`

### 2.2 엔드포인트

#### 헬스체크

```http
GET /healthz
```

**응답 예:**

```json
{ "status": "healthy", "service": "evtx-parser" }
```

#### EVTX 파싱 (파일 업로드)

```http
POST /parse
Content-Type: multipart/form-data
```

| 구분 | 이름 | 필수 | 설명 |
|------|------|------|------|
| Form | `file` | O | .evtx 파일 (multipart) |
| Query | `maxEvents` | X | 최대 이벤트 수 |
| Query | `offset` | X | 건너뛸 이벤트 수 (기본 0) |

#### EVTX 파싱 (파일 경로)

```http
POST /parse?filePath={절대경로}
```

| 구분 | 이름 | 필수 | 설명 |
|------|------|------|------|
| Query | `filePath` | O | 서버가 접근 가능한 EVTX 파일 절대 경로 |
| Query | `maxEvents` | X | 최대 이벤트 수 |
| Query | `offset` | X | 건너뛸 이벤트 수 (기본 0) |

- `file` 업로드와 `filePath`는 **동시 사용 불가**. 둘 중 하나만 사용.

### 2.3 응답 형식 (JSON)

```json
{
  "events": [
    {
      "eventId": 4624,
      "level": "Information",
      "timeCreated": "2024-01-01T12:34:56Z",
      "provider": "Microsoft-Windows-Security-Auditing",
      "computer": "WINHOST",
      "channel": "Security",
      "message": "SubjectUserName=admin | SubjectDomainName=DOMAIN",
      "rawXml": "<Event xmlns='...'>...</Event>"
    }
  ],
  "count": 1,
  "totalCount": 1
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `events` | `Array` | 파싱된 이벤트 목록 |
| `count` | `Integer` | 본 응답에 포함된 이벤트 수 |
| `totalCount` | `Integer` | (현재 구현에서 count와 동일) |

### 2.4 에러 응답

- **400**: `file` / `filePath` 누락 또는 둘 다 지정
- **404**: `filePath`에 해당하는 파일 없음
- **500**: 파싱 실패, 서버 오류

---

## 3. Spring 설정

### 3.1 의존성 (Maven)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

- WebClient 사용을 위해 **WebFlux** 필요.  
- Resilience4j(재시도/회로차단기) 사용 시 `resilience4j-spring-boot3` 등 추가.

### 3.2 application.yml

```yaml
evtx:
  service:
    url: http://evtx-service:8081   # Docker 내부. 로컬이면 http://localhost:8081
    timeoutMs: 30000
    retry:
      maxAttempts: 3
      waitDuration: 1000
    circuitBreaker:
      enabled: true
      failureRateThreshold: 50
      waitDurationInOpenState: 10000
      slidingWindowSize: 10

spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
```

### 3.3 설정 Properties 클래스 예시

```java
@Data
@Component
@ConfigurationProperties(prefix = "evtx.service")
public class EvtxServiceProperties {
    private String url = "http://localhost:8081";
    private int timeoutMs = 30000;
    private Retry retry = new Retry();
    private CircuitBreaker circuitBreaker = new CircuitBreaker();

    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private int waitDuration = 1000;
    }

    @Data
    public static class CircuitBreaker {
        private boolean enabled = true;
        private int failureRateThreshold = 50;
        private int waitDurationInOpenState = 10000;
        private int slidingWindowSize = 10;
    }
}
```

---

## 4. DTO / 모델

### 4.1 이벤트 (Event)

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    private Integer eventId;
    private String level;
    private String timeCreated;   // ISO-8601
    private String provider;
    private String computer;
    private String channel;
    private String message;
    private String rawXml;
}
```

- Jackson `@JsonProperty` 사용 시 API 필드명과 일치시키면 됨.

### 4.2 파싱 응답 (ParseResponse)

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParseResponse {
    private List<Event> events;
    private Integer count;
    private Integer totalCount;
}
```

---

## 5. WebClient 연동 예시

### 5.1 WebClient Bean

```java
@Configuration
@RequiredArgsConstructor
public class EvtxWebClientConfig {
    private final EvtxServiceProperties props;

    @Bean(name = "evtxWebClient")
    public WebClient evtxWebClient() {
        return WebClient.builder()
                .baseUrl(props.getUrl())
                .codecs(c -> c.defaultCodecs().maxInMemorySize(100 * 1024 * 1024)) // 100MB
                .build();
    }
}
```

### 5.2 파일 업로드로 파싱

- **Spring MVC**: `MultipartFile` 사용.
- **Spring WebFlux**: `org.springframework.http.codec.multipart.FilePart` 사용.  
  simple-evtx 샘플은 WebFlux + `FilePart` 기준.

**MVC 예시 (MultipartFile):**

```java
@Service
@RequiredArgsConstructor
public class EvtxParserService {
    @Qualifier("evtxWebClient")
    private final WebClient webClient;
    private final EvtxServiceProperties props;

    public Mono<List<Event>> parse(MultipartFile file, Integer maxEvents, Integer offset)
            throws IOException {
        Path tmp = Files.createTempFile("evtx_", ".evtx");
        file.transferTo(tmp.toFile());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(tmp.toFile()));

        return webClient.post()
                .uri(ub -> {
                    ub.path("/parse");
                    if (maxEvents != null) ub.queryParam("maxEvents", maxEvents);
                    if (offset != null) ub.queryParam("offset", offset);
                    return ub.build();
                })
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .bodyToMono(ParseResponse.class)
                .timeout(Duration.ofMillis(props.getTimeoutMs()))
                .map(ParseResponse::getEvents)
                .doFinally(s -> {
                    try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
                });
    }
}
```

### 5.3 파일 경로로 파싱

```java
public Mono<List<Event>> parseByPath(String filePath, Integer maxEvents, Integer offset) {
    return webClient.post()
            .uri(ub -> {
                ub.path("/parse").queryParam("filePath", filePath);
                if (maxEvents != null) ub.queryParam("maxEvents", maxEvents);
                if (offset != null) ub.queryParam("offset", offset);
                return ub.build();
            })
            .retrieve()
            .bodyToMono(ParseResponse.class)
            .timeout(Duration.ofMillis(props.getTimeoutMs()))
            .map(ParseResponse::getEvents);
}
```

- `filePath`는 **evtx-service가 접근 가능한 경로**여야 함 (같은 Docker 볼륨 등).

---

## 6. 에러 처리 / 복원력

### 6.1 타임아웃

- `WebClient` 호출에 `.timeout(Duration.ofMillis(evtx.service.timeoutMs))` 적용 권장.
- 대용량 EVTX는 `timeoutMs` 증가 검토.

### 6.2 재시도

- **5xx**만 재시도 권장. 4xx는 재시도 없이 실패 처리.
- `reactor.util.retry.RetrySpec.backoff(maxAttempts, Duration.ofMillis(waitMs))` 등 사용 가능.

### 6.3 Circuit Breaker (Resilience4j)

- evtx-service 장애 시 연쇄 실패 방지용.
- `@CircuitBreaker` + fallback에서 빈 리스트 또는 에러 응답 반환 패턴 적용 가능.

### 6.4 임시 파일 정리

- 업로드 파일을 임시 저장한 경우, **파싱 호출 완료/실패 후 반드시 삭제** (`doFinally`, `try-finally` 등).

---

## 7. 배포·네트워크

### 7.1 Docker Compose

- evtx-service와 Spring 앱을 **같은 Docker 네트워크**에 두면,  
  `evtx.service.url=http://evtx-service:8081` 로 호출.

### 7.2 Kubernetes

- evtx-service를 Service로 노출한 뒤, `http://evtx-service:8081` 또는 ClusterIP URL 사용.

### 7.3 로컬 개발

- evtx-service만 로컬 실행 시 `http://localhost:8081`.
- Spring은 `evtx.service.url=http://localhost:8081` 로 설정.

---

## 8. 체크리스트

연동 시 아래를 확인하세요.

- [ ] `evtx.service.url`이 실행 환경(로컬/Docker/K8s)에 맞는지
- [ ] `spring.servlet.multipart.max-file-size` 등으로 업로드 크기 제한 충분한지
- [ ] 타임아웃·재시도·Circuit Breaker 적용 여부
- [ ] 업로드 임시 파일 삭제 처리
- [ ] evtx-service `/healthz`로 정상 기동 여부 확인

---

## 9. 참고

- **simple-evtx** 샘플: [spring-boot-app](../spring-boot-app)  
  - `EvtxParserService`, `EvtxController`, `EvtxServiceProperties`, `WebClientConfig` 참고.
- **evtx-service** API: [루트 README](../README.md) 내 API 사용법.  
- **문서**: [docs/README.md](README.md)
