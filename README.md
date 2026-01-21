# Windows Event Log Analysis Web System (EVLO)

> **"Windows 이벤트 로그를 사람이 이해할 수 있게 번역해주는 웹 도구"**

Windows 시스템에서 추출한 **EVTX 로그 파일**을 웹에서 업로드하여 이벤트를 **검색·필터링·분석·요약**하고, 장애 분석, 보안 감사, 운영 이슈 파악을 **비전문가도 쉽게** 할 수 있도록 지원하는 웹 시스템입니다.

## 🎯 프로젝트 개요

### 목적
- Windows EVTX 로그 파일 웹 업로드 및 분석
- 이벤트 검색, 필터링, 분석, AI 기반 요약
- 장애 분석, 보안 감사, 운영 이슈 파악 지원
- 비전문가도 쉽게 사용할 수 있는 직관적인 인터페이스

### 대상 사용자
- 기술지원 / 운영 / 인프라 담당자
- 고객사 PC 로그를 분석해야 하는 개발자
- Windows 이벤트 로그에 익숙하지 않은 비전공자

## ✨ 주요 기능

### 1. 로그 업로드 및 파싱
- EVTX 파일 업로드 (drag & drop)
- 다중 파일 업로드 지원
- 스트리밍 처리로 대용량 파일 지원 (최대 200MB)
- 실시간 파싱 진행률 표시

### 2. 로그 조회 및 검색
- 테이블 기반 이벤트 리스트
- 컬럼 정렬 및 페이징
- 다양한 필터 옵션:
  - 기간 필터
  - 로그 종류 (System/Application/Security)
  - Event Level (Information/Warning/Error/Critical)
  - Event ID
  - 키워드 검색 (Message)

### 3. 분석 기능
- Event ID별 발생 빈도 통계
- Error/Critical Top N 분석
- 시간대별 집중 발생 이벤트 분석
- Chart.js 기반 시각화

### 4. AI 기반 분석 (Spring AI)
- 로그 요약
- 장애 원인 추정
- 초보자용 이벤트 설명

### 5. 결과 내보내기
- CSV / JSON 다운로드
- PDF 분석 보고서 생성
- 웹 분석 보고서 PDF 저장

## 🛠 기술 스택

### Backend
- **Framework**: Spring Boot 3.x
- **Web**: Spring WebFlux (Reactive)
- **View**: Thymeleaf
- **ORM**: Spring Data JPA (Hibernate)
- **Cache**: Redis (Reactive)
- **AI**: Spring AI (OpenAI)
- **PDF**: OpenPDF / iText / Apache PDFBox
- **Build**: Gradle
- **Java**: 17+

### Frontend
- **Template**: Thymeleaf (Server-side Rendering)
- **UI Framework**: Tailwind CSS
- **Chart**: Chart.js
- **Design**: TOSS 스타일 참고 (미니멀, 현대적)

### Database
- **Database**: MariaDB
- **Batch Processing**: JPA batch insert with flush/clear

### Infrastructure
- **Container**: Docker & Docker Compose
- **Reverse Proxy**: Nginx
- **HTTPS**: TLS 1.3 (Let's Encrypt)

## 📋 분석 대상 로그

### Windows EVTX 로그 종류
- **System**: OS, 드라이버, 서비스 이벤트
- **Application**: 응용프로그램 로그
- **Security**: 보안/감사 로그
- **Setup**: 업데이트, 설치 로그
- **ForwardedEvents**: 수집된 원격 로그

### Event Level
- Information
- Warning
- Error
- Critical

## 🚀 시작하기

### 사전 요구사항
- Java 17 이상
- Gradle 7.x 이상
- Docker & Docker Compose (MariaDB, Redis 실행용)
- Node.js (Tailwind CSS 빌드용, 선택)

### 설치 및 실행

#### 1. 프로젝트 클론
```bash
git clone <repository-url>
cd EVLO
```

#### 2. Docker Compose로 데이터베이스 실행
```bash
docker-compose up -d
```

#### 3. 설정 파일 구성
`src/main/resources/application.yml` 파일을 생성하고 설정:
```yaml
spring:
  datasource:
    url: jdbc:mariadb://localhost:3306/evlo
    username: your_username
    password: your_password
  
  data:
    redis:
      host: localhost
      port: 6379
```

#### 4. 애플리케이션 실행
```bash
./gradlew bootRun
```

또는 IDE에서 `EvloApplication` 클래스를 실행

#### 5. 웹 브라우저 접속
```
http://localhost:8080
```

## 📁 프로젝트 구조

```
EVLO/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/evlo/
│   │   │       ├── controller/    # WebFlux RouterFunction or @RestController
│   │   │       ├── service/       # 비즈니스 로직
│   │   │       ├── repository/    # JPA Repository
│   │   │       ├── entity/        # JPA Entity
│   │   │       ├── dto/           # Request/Response DTO
│   │   │       ├── config/        # 설정 클래스
│   │   │       ├── exception/     # 커스텀 예외
│   │   │       └── parser/        # EVTX 파서
│   │   └── resources/
│   │       ├── templates/         # Thymeleaf 템플릿
│   │       ├── static/            # CSS, JS
│   │       └── application.yml
│   └── test/
├── docs/                           # 문서
│   ├── TASK.md                    # 개발 작업 목록
│   └── windows_이벤트_로그_분석_웹_기능_정의서_아키텍처.md
├── scripts/                        # 유틸리티 스크립트
├── docker-compose.yml              # Docker Compose 설정
├── build.gradle                    # Gradle 빌드 설정
└── README.md
```

## 🔧 개발 가이드

### 코드 컨벤션
프로젝트의 코드 스타일 및 컨벤션은 [.cursorrules](.cursorrules) 파일을 참고하세요.

주요 규칙:
- **Java**: camelCase (변수, 메서드), PascalCase (클래스)
- **WebFlux**: Mono<T>, Flux<T> 사용 (Non-blocking)
- **JPA**: Batch 처리 필수 (500-1000건마다 flush/clear)
- **Frontend**: TOSS 스타일 디자인 참고

### Git 커밋 메시지
TASK.md 기반 커밋 메시지 자동 제안이 설정되어 있습니다.

자세한 내용은 [SETUP-GIT.md](SETUP-GIT.md)를 참고하세요.

```bash
# 커밋 전 제안 확인 (PowerShell)
.\scripts\suggest-commit.ps1

# Git 커밋
git add .
git commit
```

### 개발 작업 목록
상세한 개발 작업 목록은 [docs/TASK.md](docs/TASK.md)를 참고하세요.

## 📊 아키텍처

```
[Browser]
   │ HTTPS (TLS)
   ▼
[Reverse Proxy / Nginx]
   │ TLS Termination
   ▼
[Spring Boot Backend]
   │
   ├─ WebFlux Controller
   ├─ EVTX Parsing & Batch Save
   │     └─ flush / clear
   ├─ AI Analysis
   ├─ Query API
   │
   ├─ Redis (Cache & Progress)
   └─ MariaDB (Data Storage)
```

### 주요 설계 원칙
- **Reactive**: WebFlux 기반 Non-blocking 처리
- **Batch Processing**: 대량 데이터 처리를 위한 JPA Batch Insert
- **Streaming**: 대용량 파일 스트리밍 처리
- **Caching**: Redis를 활용한 검색 조건 및 메타데이터 캐싱

## 🧪 테스트

```bash
# 단위 테스트 실행
./gradlew test

# 통합 테스트 실행
./gradlew integrationTest
```

## 🐳 Docker

### Docker Compose로 전체 스택 실행
```bash
# MariaDB, Redis 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 중지
docker-compose down
```

## 📚 문서

- [아키텍처 문서](docs/windows_이벤트_로그_분석_웹_기능_정의서_아키텍처.md) - 시스템 아키텍처 및 설계
- [TASK.md](docs/TASK.md) - 개발 작업 목록
- [SETUP-GIT.md](SETUP-GIT.md) - Git 커밋 메시지 자동 제안 설정
- [.cursorrules](.cursorrules) - 코드 스타일 및 컨벤션

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'feat: Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### 커밋 메시지 형식
[Conventional Commits](https://www.conventionalcommits.org/) 형식을 따릅니다:

```
<type>(<scope>): <subject>

<body>
```

**타입**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `perf`

## 📝 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.

## 👥 기여자

- 프로젝트 관리자

## 🔮 향후 계획

- [ ] Linux 로그(syslog) 지원
- [ ] 실시간 에이전트 연동
- [ ] 고객사별 로그 히스토리
- [ ] 장애 패턴 학습

---

**한 줄 요약**: Windows 이벤트 로그를 사람이 이해할 수 있게 번역해주는 웹 도구
