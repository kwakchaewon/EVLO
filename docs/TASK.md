# Windows 이벤트 로그 분석 웹 시스템 - 개발 작업 목록

## 📊 작업 상태 요약
- **[x] 완료**: 작업 완료
- **[-] 진행 중**: 현재 작업 중
- **[ ] 예정**: 작업 예정

### 현재 진행 상황
- **Week 2**: AI 기반 로그 요약 기능 구현 완료 ✅
- **다음 작업**: PDF 분석 보고서 생성 기능

---

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
- [x] Spring Boot 3.x 프로젝트 생성 (Gradle)
  - [x] Spring Initializr 또는 수동 생성
  - [x] 프로젝트 구조 설정
- [x] WebFlux 의존성 추가
  - [x] spring-boot-starter-webflux
  - [x] spring-boot-starter-thymeleaf (WebFlux용)
- [x] JPA + Hibernate 설정
  - [x] spring-boot-starter-data-jpa
  - [x] MariaDB Driver 의존성
- [x] Redis 의존성 및 설정
  - [x] spring-boot-starter-data-redis-reactive
  - [x] Lettuce 클라이언트 설정
- [x] Thymeleaf 설정
  - [x] Thymeleaf Layout Dialect (선택)
  - [x] Thymeleaf 템플릿 디렉토리 구조
- [x] application.yml 기본 설정
  - [x] 프로파일 설정 (dev, prod)
  - [x] 데이터베이스 연결 설정
  - [x] Redis 연결 설정
  - [x] 로깅 설정

**다음 작업 정보:**
- Event 엔티티 설계 필요
- 필드: EventID, Level, TimeCreated, Provider, Computer, Message
- 인덱스: TimeCreated, EventID, Level
- Enum 타입: EventLevel (Information/Warning/Error/Critical), LogChannel (System/Application/Security/Setup/ForwardedEvents)

### 2.2 데이터베이스 설계
- [x] Flyway 데이터베이스 마이그레이션 도구 설정
  - [x] Flyway 의존성 추가
  - [x] Flyway 기본 설정 (application.yml)
  - [x] 마이그레이션 스크립트 디렉토리 구조 준비 (src/main/resources/db/migration)
- [x] Event 엔티티 설계
  - [x] 필드 정의 (EventID, Level, TimeCreated, Provider, Computer, Message 등)
  - [x] 인덱스 설계 (TimeCreated, EventID, Level, Channel)
  - [x] Enum 타입 정의 (EventLevel, LogChannel, ParsingStatus)
- [x] LogFile 메타정보 엔티티 설계
  - [x] 파일명, 크기, 업로드 시간
  - [x] 파싱 상태 (진행중, 완료, 실패)
  - [x] Event 엔티티와 OneToMany 관계
- [x] Flyway 마이그레이션 스크립트 작성
  - [x] V2__create_log_files_table.sql
  - [x] V3__create_events_table.sql
  - [x] 인덱스 및 외래키 설정
- [x] JPA Repository 생성
  - [x] EventRepository 인터페이스
    - [x] 기본 CRUD 메서드
    - [x] 검색/필터링 메서드 (기간, Level, Channel, Event ID, 키워드)
    - [x] 복합 검색 메서드 (findByFilters)
    - [x] Error/Critical Top N 조회
    - [x] Event ID별 발생 빈도 조회
    - [x] 시간대별 집중 발생 이벤트 분석
  - [x] LogFileRepository 인터페이스
    - [x] 기본 CRUD 메서드
    - [x] 파싱 상태별 조회
    - [x] 파일명 검색
    - [x] 업로드 시간 범위 조회
    - [x] 최근 업로드 파일 조회
  - [x] Custom Query 메서드 정의

**다음 작업 정보:**
- EVTX 파서 라이브러리 조사 및 선정 필요
- Java용 EVTX 파서 옵션: evtx4j, palindromicity/evtx, jSQLParser 등
- EVTX → Event 엔티티 변환 로직 구현 필요
- [x] Hibernate batch 설정 (이미 완료)
  - [x] `spring.jpa.properties.hibernate.jdbc.batch_size: 1000`
  - [x] `spring.jpa.properties.hibernate.order_inserts: true`
  - [x] `spring.jpa.properties.hibernate.order_updates: true`

**다음 작업 정보:**
- JPA Repository 인터페이스 생성 필요
- EventRepository: 기본 CRUD + 검색/필터링 메서드
- LogFileRepository: 기본 CRUD + 파싱 상태별 조회 메서드

### 2.3 EVTX 파서 PoC
- [x] EVTX 파싱 라이브러리 선정 및 통합
  - [x] palindromicity/simple-evtx 라이브러리 선택
  - [x] 의존성 추가 (build.gradle)
- [x] EVTX → Event 엔티티 변환 로직 구현
  - [x] 필수 필드 매핑 (EventID, Level, TimeCreated, Provider, Computer, Message)
  - [x] 로그 종류(Channel) 추출 로직
  - [x] 예외 처리 (EvtxParsingException)
- [x] 파싱 서비스 클래스 작성
  - [x] EvtxParserService 구현
  - [ ] 스트리밍 파싱 메서드 (추후 구현)
  - [ ] 진행률 추적 기능 (추후 구현)
- [ ] 단위 테스트 작성
  - [ ] 샘플 EVTX 파일로 파싱 테스트
  - [ ] 필드 매핑 검증

**다음 작업 정보:**
- 단위 테스트 작성 필요
- 스트리밍 파싱 및 진행률 추적 기능 (파일 업로드 기능과 통합 시 구현)
- 샘플 EVTX 파일 준비 필요

### 2.4 파일 업로드 기능
- [x] WebFlux MultipartFile 업로드 API 구현
  - [x] POST /api/upload 엔드포인트
  - [x] POST /api/upload/multiple (다중 파일 업로드 지원)
  - [x] Mono<ResponseEntity> 반환 (boundedElastic() 사용)
- [x] 파일 크기 제한 설정 (200MB)
  - [x] application.yml에 설정 (완료)
  - [x] 업로드 시 파일 크기 검증
- [x] 파일 검증 (EVTX 확장자 체크)
  - [x] 파일 확장자 검증 로직
  - [x] 잘못된 파일 형식 에러 처리 (FileValidationException)
- [x] 임시 저장 처리
  - [x] 업로드된 파일 임시 저장 경로 설정 (app.upload.temp-dir)
  - [x] 파일 처리 완료 후 삭제 정책 (finally 블록에서 삭제)

**다음 작업 정보:**
- 스트리밍 처리 및 Batch 저장 최적화 필요
- 진행률 추적 기능 (Redis 활용) 구현 필요
- 파일 업로드 UI 구현 필요

### 2.5 스트리밍 처리 및 Batch 저장
- [x] WebFlux 스트리밍 업로드 처리
  - [x] Mono 기반 Non-blocking 처리
  - [x] 메모리 효율적인 파일 처리 (임시 파일 사용)
- [x] EVTX 파싱 스트리밍 구현
  - [x] 대용량 파일 스트리밍 파싱 (EvtxParserService)
  - [x] 진행률 Redis 저장 (100건마다 또는 배치 크기마다)
- [x] JPA batch insert 구현
  - [x] 500-1000건마다 flush/clear 로직 (설정 가능)
  - [x] EntityManager 사용 (Service Layer)
  - [x] @Transactional 설정
- [x] boundedElastic()로 Blocking 처리 분리
  - [x] EVTX 파싱을 별도 스레드풀에서 실행 (processFileAsync)
  - [x] Controller에서 Non-blocking 유지

**다음 작업 정보:**
- 기본 UI 구현 필요 (Thymeleaf, Tailwind CSS)
- 로그 업로드 페이지 구현
- 진행률 표시 UI 구현 (프론트엔드)

### 2.6 기본 UI (Thymeleaf)
- [x] Tailwind CSS 설정 (또는 Alpine.js)
  - [x] Tailwind CSS CDN 설정
  - [x] 커스텀 디자인 토큰 정의 (TOSS 컬러, 타이포그래피)
- [x] TOSS 스타일 디자인 시스템 기본 구성
  - [x] 컬러 팔레트 정의 (toss-blue, toss-gray 시리즈)
  - [x] 타이포그래피 설정 (TOSS 폰트)
  - [x] 레이아웃 컴포넌트 (base.html - 헤더, 네비게이션, 푸터)
- [x] 로그 업로드 페이지 구현
  - [x] Drag & Drop UI
  - [x] 파일 선택 버튼
  - [x] 업로드 결과 표시
  - [x] 다중 파일 업로드 UI
- [x] 기본 로그 리스트 화면 구현
  - [x] 테이블 UI (TOSS 스타일)
  - [x] 컬럼 구성 (EventID, Level, TimeCreated, Provider, Computer, Message)
  - [x] 반응형 디자인
- [x] 페이징 구현
  - [x] 서버 사이드 페이징
  - [x] 페이지네이션 UI
  - [x] 컬럼 정렬 기능

**다음 작업 정보:**
- 검색/필터링 기능 구현 필요
- 검색 조건 DTO 설계
- 필터 UI 구현

---

## Week 2: 검색/필터링 및 분석 기능

### 3.1 검색 및 필터링
- [x] 검색 조건 DTO 설계
  - [x] EventSearchRequest DTO
  - [x] 검증 어노테이션 추가 (@Min)
- [x] 기간 필터 구현
  - [x] LocalDateTime 기반 날짜 범위 필터
  - [x] 시간대 처리 (datetime-local)
- [x] 로그 종류 필터
  - [x] System/Application/Security/Setup/ForwardedEvents
  - [x] 다중 선택 지원 (multiple select)
- [x] Event Level 필터
  - [x] Information/Warning/Error/Critical
  - [x] 다중 선택 지원 (multiple select)
- [x] Event ID 필터
  - [x] 다중 Event ID 필터 (쉼표로 구분)
  - [ ] 범위 검색 지원 (추후 구현 가능)
- [x] Message 키워드 검색
  - [x] LIKE 검색 (대소문자 구분 안 함)
  - [ ] 대소문자 구분 옵션 (추후 구현 가능)
- [x] 필터 UI 구현
  - [x] 필터 폼 (TOSS 스타일)
  - [x] 동적 필터 조합 (복합 검색)
  - [x] 필터 초기화 기능

**다음 작업 정보:**
- Redis 캐싱 구현 필요
- 자주 사용되는 검색 조건 캐싱
- 파싱 진행률 조회 API 개선

### 3.2 Redis 캐싱
- [x] 업로드 파일 메타정보 Redis 저장
  - [x] 파일 ID를 키로 사용 (file:meta:{fileId})
  - [x] TTL 설정 (24시간)
- [x] 파싱 진행률 Redis 저장
  - [x] 진행률 조회 API 구현 (/api/progress/{fileId})
  - [ ] WebSocket 또는 SSE로 실시간 진행률 전송 (추후 구현)
- [x] 검색 조건 캐시 구현
  - [x] 자주 사용되는 검색 조건 캐싱 (CacheService)
  - [x] 캐시 키 전략 수립 (검색 조건 기반)
  - [x] 검색 결과 캐시 저장/조회
  - [x] 검색 카운트 통계
- [x] Redis 설정 및 의존성 추가
  - [x] RedisConfig 설정
  - [x] Jackson 의존성 추가

**다음 작업 정보:**
- 분석 기능 구현 필요
- Event ID별 발생 빈도 통계
- Error/Critical Top N 분석
- 시간대별 집중 발생 이벤트 분석
  - [ ] ReactiveRedisTemplate 설정
  - [ ] Redis 직렬화 설정

### 3.3 분석 기능
- [x] Event ID별 발생 빈도 통계 API
  - [x] GET /api/analysis/event-frequency
  - [x] 그룹화 및 정렬 (빈도순)
  - [x] 차트용 데이터 형식 (EventFrequencyResponse)
  - [x] limit 파라미터로 결과 개수 제한
- [x] Error/Critical Top N 조회 API
  - [x] GET /api/analysis/errors-top
  - [x] N 파라미터로 개수 지정
  - [x] 시간순 정렬 (최신순)
- [x] 시간대별 집중 발생 이벤트 분석
  - [x] 시간 단위 그룹화 (1시간 단위)
  - [x] 통계 데이터 반환 (TimeBasedAnalysisResponse)
  - [x] 전체 이벤트 및 특정 Event ID별 분석 지원
- [x] 통계 데이터 DTO 설계
  - [x] EventFrequencyResponse
  - [x] TimeBasedAnalysisResponse

**다음 작업 정보:**
- AI 기반 로그 요약 기능 (Spring AI)
- PDF 분석 보고서 생성 기능
- CSV/JSON 내보내기 기능

### 3.4 AI 로그 요약
- [x] Spring AI 의존성 추가 및 설정
  - [x] spring-ai-openai-spring-boot-starter (이미 추가됨)
  - [x] OpenAI API 키 설정 (환경변수: OPENAI_API_KEY)
- [x] OpenAI API 연동 (또는 다른 AI 모델)
  - [x] ChatClient 설정 (자동 구성)
  - [x] 모델 선택 (gpt-3.5-turbo)
- [x] 로그 요약 서비스 구현
  - [x] 전체 로그 요약 기능 (AiSummaryService)
  - [x] Error/Critical 이벤트 중심 요약
  - [x] 프롬프트 엔지니어링 (한국어 요약)
- [x] 장애 원인 추정 기능
  - [x] Error 이벤트 패턴 분석
  - [x] 원인 추론 프롬프트
- [x] 초보자용 설명 생성 기능
  - [x] Event ID별 설명 생성
  - [x] 사용자 친화적 언어로 변환

**다음 작업 정보:**
- PDF 분석 보고서 생성 기능
- CSV/JSON 내보내기 기능
- 전역 예외 처리 및 에러 페이지

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

### 0.1 프로젝트 문서화
- [x] 프로젝트 README 초안 작성
  - [x] 프로젝트 개요 및 목적 설명
  - [x] 주요 기능 및 기술 스택 소개
  - [x] 설치 및 실행 가이드
  - [x] 개발 가이드 및 문서 링크
- [x] Git 커밋 메시지 자동 제안 설정
  - [x] 커밋 템플릿 (.gitmessage) 생성
  - [x] Git Hook (prepare-commit-msg) 설정
  - [x] 커밋 메시지 제안 스크립트 작성 (Bash/PowerShell)
  - [x] Git 설정 가이드 문서 작성 (SETUP-GIT.md, README-GIT.md)
- [x] Cursor Rules 작성
  - [x] 코드 스타일 및 컨벤션 정의
  - [x] 필수 규칙 및 금지 사항 명시
- [x] TASK.md 작업 트래커 구성
  - [x] 작업 상태 표시 방법 정의 ([x] 완료, [-] 진행 중, [ ] 예정)
  - [x] 작업 상태 요약 섹션 추가

**다음 작업 정보:**
- Spring Boot 3.x 프로젝트 생성 필요
- Gradle 빌드 파일 작성
- 프로젝트 기본 패키지 구조 설정 (com.evlo.{domain})

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

## 📝 작업 트래커 업데이트 가이드

작업 완료 시 TASK.md를 업데이트하세요:

1. **완료된 작업**: `[ ]` → `[x]`
2. **진행 중인 작업**: `[ ]` → `[-]`
3. **다음 작업 정보 추가**: 해당 섹션 하단에 컴팩트한 정보 기록

**예시:**
```markdown
### 2.1 프로젝트 세팅
- [-] Spring Boot 3.x 프로젝트 생성 (Gradle)
  - [x] Spring Initializr 또는 수동 생성
  - [-] 프로젝트 구조 설정

**다음 작업 정보:**
- build.gradle 파일 작성 필요
- 기본 패키지 구조 생성 (com.evlo)
- application.yml 기본 설정 파일 생성
```

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
