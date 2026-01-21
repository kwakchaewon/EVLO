# Windows 이벤트 로그 분석 웹 시스템 - 개발 작업 목록

## 프로젝트 개요

### 목적
Windows 시스템에서 추출한 **EVTX 로그 파일**을 웹에서 업로드하여 이벤트를 **검색·필터링·분석·요약**하고, 장애 분석, 보안 감사, 운영 이슈 파악을 **비전문가도 쉽게** 할 수 있도록 지원하는 웹 시스템

### 기술 스택
- **Backend**: Spring Boot 3.x, Spring WebFlux, Spring Data JPA, Redis, Spring AI
- **Frontend**: Thymeleaf, Tailwind CSS (또는 Alpine.js), Chart.js
- **Database**: MariaDB
- **Build**: Gradle
- **Infra**: Docker, Nginx
- **PDF**: OpenPDF / iText / Apache PDFBox

### 개발 환경 요구사항
- Java 17+
- Spring Boot 3.x
- Gradle
- Docker & Docker Compose (MariaDB, Redis)
- Node.js (Tailwind CSS 빌드용, 선택)

---

## Week 1: 프로젝트 초기 설정 및 기본 기능

### 2.1 프로젝트 세팅
- [ ] Spring Boot 3.x 프로젝트 생성 (Gradle)
  - [ ] Spring Initializr 또는 수동 생성
  - [ ] 프로젝트 구조 설정
- [ ] WebFlux 의존성 추가
  - [ ] spring-boot-starter-webflux
  - [ ] spring-boot-starter-thymeleaf (WebFlux용)
- [ ] JPA + Hibernate 설정
  - [ ] spring-boot-starter-data-jpa
  - [ ] MariaDB Driver 의존성
- [ ] Redis 의존성 및 설정
  - [ ] spring-boot-starter-data-redis-reactive
  - [ ] Lettuce 클라이언트 설정
- [ ] Thymeleaf 설정
  - [ ] Thymeleaf Layout Dialect (선택)
  - [ ] Thymeleaf 템플릿 디렉토리 구조
- [ ] application.yml 기본 설정
  - [ ] 프로파일 설정 (dev, prod)
  - [ ] 데이터베이스 연결 설정
  - [ ] Redis 연결 설정
  - [ ] 로깅 설정

### 2.2 데이터베이스 설계
- [ ] Event 엔티티 설계
  - [ ] 필드 정의 (EventID, Level, TimeCreated, Provider, Computer, Message 등)
  - [ ] 인덱스 설계 (TimeCreated, EventID, Level)
  - [ ] Enum 타입 정의 (EventLevel, LogChannel)
- [ ] LogFile 메타정보 엔티티 설계
  - [ ] 파일명, 크기, 업로드 시간
  - [ ] 파싱 상태 (진행중, 완료, 실패)
  - [ ] Event 엔티티와 OneToMany 관계
- [ ] JPA Repository 생성
  - [ ] EventRepository 인터페이스
  - [ ] LogFileRepository 인터페이스
  - [ ] Custom Query 메서드 정의
- [ ] Hibernate batch 설정
  - [ ] `spring.jpa.properties.hibernate.jdbc.batch_size: 1000`
  - [ ] `spring.jpa.properties.hibernate.order_inserts: true`
  - [ ] `spring.jpa.properties.hibernate.order_updates: true`

### 2.3 EVTX 파서 PoC
- [ ] EVTX 파싱 라이브러리 선정 및 통합
  - [ ] evtx4j 또는 다른 Java EVTX 파서 조사
  - [ ] 의존성 추가 및 테스트
- [ ] EVTX → Event 엔티티 변환 로직 구현
  - [ ] 필수 필드 매핑 (EventID, Level, TimeCreated, Provider, Computer, Message)
  - [ ] 로그 종류(Channel) 추출 로직
  - [ ] 예외 처리
- [ ] 파싱 서비스 클래스 작성
  - [ ] EvtxParserService 인터페이스 및 구현
  - [ ] 스트리밍 파싱 메서드
  - [ ] 진행률 추적 기능
- [ ] 단위 테스트 작성
  - [ ] 샘플 EVTX 파일로 파싱 테스트
  - [ ] 필드 매핑 검증

### 2.4 파일 업로드 기능
- [ ] WebFlux MultipartFile 업로드 API 구현
  - [ ] POST /api/upload 엔드포인트
  - [ ] 다중 파일 업로드 지원
  - [ ] Mono<ServerResponse> 반환
- [ ] 파일 크기 제한 설정 (200MB)
  - [ ] application.yml에 설정
  - [ ] 업로드 시 파일 크기 검증
- [ ] 파일 검증 (EVTX 확장자 체크)
  - [ ] 파일 확장자 검증 로직
  - [ ] 잘못된 파일 형식 에러 처리
- [ ] 임시 저장 처리
  - [ ] 업로드된 파일 임시 저장 경로 설정
  - [ ] 파일 처리 완료 후 삭제 정책

### 2.5 스트리밍 처리 및 Batch 저장
- [ ] WebFlux 스트리밍 업로드 처리
  - [ ] DataBuffer를 이용한 스트리밍
  - [ ] 메모리 효율적인 파일 처리
- [ ] EVTX 파싱 스트리밍 구현
  - [ ] 대용량 파일 스트리밍 파싱
  - [ ] 진행률 Redis 저장
- [ ] JPA batch insert 구현
  - [ ] 500-1000건마다 flush/clear 로직
  - [ ] EntityManager 사용 (Service Layer)
  - [ ] @Transactional 설정
- [ ] boundedElastic()로 Blocking 처리 분리
  - [ ] EVTX 파싱을 별도 스레드풀에서 실행
  - [ ] Non-blocking 유지

### 2.6 기본 UI (Thymeleaf)
- [ ] Tailwind CSS 설정 (또는 Alpine.js)
  - [ ] Tailwind CSS CDN 또는 빌드 설정
  - [ ] 커스텀 디자인 토큰 정의
- [ ] TOSS 스타일 디자인 시스템 기본 구성
  - [ ] 컬러 팔레트 정의
  - [ ] 타이포그래피 설정
  - [ ] 레이아웃 컴포넌트 (헤더, 네비게이션)
- [ ] 로그 업로드 페이지 구현
  - [ ] Drag & Drop UI
  - [ ] 파일 선택 버튼
  - [ ] 업로드 진행률 표시
  - [ ] 다중 파일 업로드 UI
- [ ] 기본 로그 리스트 화면 구현
  - [ ] 테이블 UI (TOSS 스타일)
  - [ ] 컬럼 구성 (EventID, Level, TimeCreated, Provider, Computer, Message)
  - [ ] 반응형 디자인
- [ ] 페이징 구현
  - [ ] 서버 사이드 페이징
  - [ ] 페이지네이션 UI
  - [ ] 페이지 크기 선택

---

## Week 2: 검색/필터링 및 분석 기능

### 3.1 검색 및 필터링
- [ ] 검색 조건 DTO 설계
  - [ ] EventSearchRequest DTO
  - [ ] 검증 어노테이션 추가
- [ ] 기간 필터 구현
  - [ ] LocalDateTime 기반 날짜 범위 필터
  - [ ] 시간대 처리
- [ ] 로그 종류 필터
  - [ ] System/Application/Security/Setup/ForwardedEvents
  - [ ] 다중 선택 지원
- [ ] Event Level 필터
  - [ ] Information/Warning/Error/Critical
  - [ ] 다중 선택 지원
- [ ] Event ID 필터
  - [ ] 단일 또는 다중 Event ID 필터
  - [ ] 범위 검색 지원 (선택)
- [ ] Message 키워드 검색
  - [ ] Full-text search 또는 LIKE 검색
  - [ ] 대소문자 구분 옵션
- [ ] 필터 UI 구현
  - [ ] 필터 폼 (TOSS 스타일)
  - [ ] 동적 필터 조합
  - [ ] 필터 초기화 기능

### 3.2 Redis 캐싱
- [ ] 업로드 파일 메타정보 Redis 저장
  - [ ] 파일 ID를 키로 사용
  - [ ] TTL 설정
- [ ] 파싱 진행률 Redis 저장
  - [ ] 진행률 조회 API 구현
  - [ ] WebSocket 또는 SSE로 실시간 진행률 전송 (선택)
- [ ] 검색 조건 캐시 구현
  - [ ] 자주 사용되는 검색 조건 캐싱
  - [ ] 캐시 키 전략 수립
- [ ] Redis 설정 및 의존성 추가
  - [ ] ReactiveRedisTemplate 설정
  - [ ] Redis 직렬화 설정

### 3.3 분석 기능
- [ ] Event ID별 발생 빈도 통계 API
  - [ ] GET /api/analysis/event-frequency
  - [ ] 그룹화 및 정렬
  - [ ] 차트용 데이터 형식
- [ ] Error/Critical Top N 조회 API
  - [ ] GET /api/analysis/errors-top
  - [ ] N 파라미터로 개수 지정
  - [ ] 시간대별 집계 옵션
- [ ] 시간대별 집중 발생 이벤트 분석
  - [ ] 시간 단위 그룹화 (1시간, 1일 등)
  - [ ] 통계 데이터 반환
- [ ] 통계 데이터 DTO 설계
  - [ ] EventFrequencyResponse
  - [ ] TimeBasedStatisticsResponse

### 3.4 AI 로그 요약
- [ ] Spring AI 의존성 추가 및 설정
  - [ ] spring-ai-openai-spring-boot-starter
  - [ ] OpenAI API 키 설정
- [ ] OpenAI API 연동 (또는 다른 AI 모델)
  - [ ] ChatClient 설정
  - [ ] 모델 선택 (GPT-4, GPT-3.5-turbo 등)
- [ ] 로그 요약 서비스 구현
  - [ ] 전체 로그 요약 기능
  - [ ] Error/Critical 이벤트 중심 요약
  - [ ] 프롬프트 엔지니어링
- [ ] 장애 원인 추정 기능
  - [ ] Error 이벤트 패턴 분석
  - [ ] 원인 추론 프롬프트
- [ ] 초보자용 설명 생성 기능
  - [ ] Event ID별 설명 생성
  - [ ] 사용자 친화적 언어로 변환

### 3.5 PDF 생성 기능
- [ ] PDF 라이브러리 선정 및 통합
  - [ ] OpenPDF 또는 iText 의존성 추가
  - [ ] 라이브러리 테스트
- [ ] 분석 보고서 PDF 템플릿 설계
  - [ ] 헤더/푸터 디자인
  - [ ] 섹션 구성 (요약, 통계, 차트, 이벤트 리스트)
  - [ ] TOSS 스타일 디자인 적용
- [ ] 차트를 포함한 PDF 생성
  - [ ] Chart.js 이미지 변환 (선택)
  - [ ] 또는 PDF 라이브러리로 직접 차트 생성
- [ ] PDF 다운로드 API 구현
  - [ ] GET /api/export/pdf
  - [ ] 현재 필터 조건 반영
  - [ ] 파일명 생성 (타임스탬프 포함)

### 3.6 내보내기 기능
- [ ] CSV 다운로드 API 구현
  - [ ] GET /api/export/csv
  - [ ] 현재 필터 조건 반영
  - [ ] 스트리밍 다운로드 (대용량)
- [ ] JSON 다운로드 API 구현
  - [ ] GET /api/export/json
  - [ ] 페이징 또는 전체 데이터
- [ ] 내보내기 UI 구현
  - [ ] 내보내기 버튼 (TOSS 스타일)
  - [ ] 형식 선택 (CSV, JSON, PDF)
  - [ ] 다운로드 진행 표시

### 3.7 성능 테스트
- [ ] 대용량 파일 (200MB) 업로드 테스트
  - [ ] 메모리 사용량 모니터링
  - [ ] 처리 시간 측정
- [ ] Batch insert 성능 측정
  - [ ] flush/clear 주기에 따른 성능 비교
  - [ ] 최적 배치 크기 결정
- [ ] 메모리 사용량 모니터링
  - [ ] JVM 힙 메모리 모니터링
  - [ ] GC 로그 분석
- [ ] 성능 최적화
  - [ ] 쿼리 최적화
  - [ ] 인덱스 튜닝
  - [ ] N+1 문제 해결

---

## Week 3: HTTPS 및 보안 설정

### 4.1 도메인 및 DNS
- [ ] 도메인 확보
  - [ ] 도메인 등록 서비스 선택
  - [ ] 도메인 구매
- [ ] DNS A 레코드 설정
  - [ ] 서버 IP와 도메인 연결
  - [ ] DNS 전파 확인

### 4.2 HTTPS/TLS 설정
- [ ] Let's Encrypt 인증서 발급 (또는 내부 CA)
  - [ ] Certbot 설치 및 설정
  - [ ] 인증서 발급
  - [ ] 자동 갱신 설정
- [ ] Spring Boot SSL 설정
  - [ ] application.yml SSL 설정
  - [ ] keystore/truststore 설정
- [ ] TLS 1.3 활성화
  - [ ] 지원 프로토콜 설정
  - [ ] 구버전 프로토콜 비활성화
- [ ] application.yml SSL 설정
  - [ ] server.ssl.* 설정 추가
  - [ ] HTTPS 리다이렉트 설정

### 4.3 Reverse Proxy (Nginx)
- [ ] Nginx 설치 및 설정
  - [ ] Nginx 설치
  - [ ] 기본 설정 파일 작성
- [ ] TLS Termination 설정
  - [ ] Nginx에서 SSL 처리
  - [ ] Backend는 HTTP로 연결
- [ ] Spring Boot 연결 설정
  - [ ] Nginx upstream 설정
  - [ ] 프록시 패스 설정
- [ ] 보안 헤더 설정
  - [ ] HSTS (Strict-Transport-Security)
  - [ ] CSP (Content-Security-Policy)
  - [ ] X-Frame-Options
  - [ ] X-Content-Type-Options
  - [ ] X-XSS-Protection

### 4.4 보안 헤더
- [ ] Spring Security 설정 (필요시)
  - [ ] 보안 필터 체인 구성
  - [ ] 인증/인가 설정 (선택)
- [ ] CORS 설정
  - [ ] 허용 오리진 설정
  - [ ] CORS 필터 구현
- [ ] 보안 헤더 추가
  - [ ] Spring Boot Actuator 보안 설정
  - [ ] 추가 보안 헤더 설정

---

## Week 4: PQC Gateway (선택/실습)

### 5.1 PQC 개념 검증
- [ ] PQC 라이브러리 조사 및 PoC
  - [ ] Kyber 알고리즘 라이브러리 조사
  - [ ] Java PQC 라이브러리 평가
- [ ] Kyber 알고리즘 실험
  - [ ] 키 생성 및 암호화 테스트
  - [ ] 성능 측정

### 5.2 Gateway 설계
- [ ] PQC Gateway 아키텍처 설계
  - [ ] Gateway 위치 및 역할 정의
  - [ ] Hybrid TLS 구성 방안
- [ ] Hybrid TLS 구성 계획
  - [ ] 기존 TLS + PQC Key Exchange 설계
  - [ ] 프로토콜 설계

### 5.3 Hybrid TLS 구현
- [ ] 기존 TLS + PQC Key Exchange 구현
  - [ ] PQC 키 교환 로직 구현
  - [ ] TLS와 통합
- [ ] Gateway ↔ Backend 보안 통신 검증
  - [ ] 통신 테스트
  - [ ] 보안 검증

---

## 부가 작업

### 6.1 에러 처리
- [ ] 전역 예외 처리 구현
  - [ ] @ControllerAdvice 클래스 작성
  - [ ] 공통 에러 응답 형식 정의
- [ ] 커스텀 예외 클래스 작성
  - [ ] EvtxParsingException
  - [ ] FileValidationException
  - [ ] DataNotFoundException 등
- [ ] 에러 응답 DTO 표준화
  - [ ] ErrorResponse DTO
  - [ ] 에러 코드 체계 정의

### 6.2 로깅
- [ ] 로깅 전략 수립
  - [ ] 로그 레벨 정의
  - [ ] 로그 포맷 결정
- [ ] SLF4J + Logback 설정
  - [ ] logback-spring.xml 작성
  - [ ] 콘솔/파일 로그 설정
  - [ ] 로그 롤링 정책
- [ ] 로그 레벨 관리
  - [ ] 프로파일별 로그 레벨 설정
  - [ ] 운영 환경 로그 레벨 조정

### 6.3 테스트
- [ ] 단위 테스트 작성
  - [ ] Service Layer 테스트
  - [ ] Repository Layer 테스트
  - [ ] Utils 클래스 테스트
  - [ ] JUnit 5, Mockito 사용
- [ ] 통합 테스트 작성
  - [ ] @SpringBootTest 활용
  - [ ] API 엔드포인트 테스트
  - [ ] WebTestClient 활용 (WebFlux)
- [ ] 테스트 커버리지 목표 설정
  - [ ] JaCoCo 설정
  - [ ] 커버리지 목표 (예: 70% 이상)

### 6.4 Docker 설정
- [ ] Dockerfile 작성
  - [ ] 멀티 스테이지 빌드
  - [ ] 최적화된 이미지 크기
- [ ] docker-compose.yml 작성
  - [ ] MariaDB 컨테이너 설정
  - [ ] Redis 컨테이너 설정
  - [ ] 애플리케이션 컨테이너 설정
  - [ ] 네트워크 구성
  - [ ] 볼륨 마운트 설정
- [ ] 컨테이너 네트워크 구성
  - [ ] Docker 네트워크 생성
  - [ ] 컨테이너 간 통신 설정

---

## 우선순위

- **P0**: 핵심 기능 (Week 1-2 기본 기능)
  - 프로젝트 세팅, DB 설계, EVTX 파서, 파일 업로드, 기본 UI
  - 검색/필터링, 분석 기능, Redis 캐싱
- **P1**: 분석 기능 (Week 2)
  - AI 로그 요약, PDF 생성, 내보내기
- **P2**: 보안 설정 (Week 3)
  - HTTPS/TLS, Nginx, 보안 헤더
- **P3**: PQC 실습 (Week 4, 선택)
  - PQC Gateway 및 Hybrid TLS

## 의존성

- 각 주차의 작업은 순차적으로 진행
- Week 2는 Week 1 완료 후 진행
- Week 3는 Week 2 완료 후 진행
- Week 4는 선택 사항
- 부가 작업은 필요에 따라 병행 가능

---

## 참고

- [아키텍처 문서](./windows_이벤트_로그_분석_웹_기능_정의서_아키텍처.md)
- JPA Batch 처리 시 flush/clear 주기는 500-1000건 권장
- TOSS 스타일 디자인 가이드 참고
- WebFlux Non-blocking 원칙 준수
