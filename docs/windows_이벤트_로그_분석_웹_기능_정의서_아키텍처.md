# Windows 이벤트 로그 분석 웹 시스템

## 1. 프로젝트 개요

### 1.1 목적
- Windows 시스템에서 추출한 **EVTX 로그 파일**을 웹에서 업로드하여
- 이벤트를 **검색·필터링·분석·요약**하고
- 장애 분석, 보안 감사, 운영 이슈 파악을 **비전문가도 쉽게** 할 수 있도록 지원

### 1.2 대상 사용자
- 기술지원 / 운영 / 인프라 담당자
- 고객사 PC 로그를 분석해야 하는 개발자
- Windows 이벤트 로그에 익숙하지 않은 비전공자

### 1.3 범위
- 로컬 PC 로그를 직접 읽는 구조 ❌
- **EVTX 파일 업로드 기반 웹 분석 도구**

---

## 2. 분석 대상 로그 범위

### 2.1 Windows EVTX 로그 종류
| 로그 종류 | 설명 | 활용 포인트 |
|---|---|---|
| System | OS, 드라이버, 서비스 이벤트 | 장애/재부팅/서비스 다운 |
| Application | 응용프로그램 로그 | 앱 크래시, 예외 |
| Security | 보안/감사 로그 | 로그인 실패, 권한 문제 |
| Setup | 업데이트, 설치 로그 | Windows Update 실패 |
| ForwardedEvents | 수집된 원격 로그 | 중앙 로그 수집 |

### 2.2 주요 Event Level
- Information
- Warning
- Error
- Critical

---

## 3. 핵심 기능 정의서

### 3.1 로그 업로드
- EVTX 파일 업로드 (drag & drop)
- 다중 파일 업로드 지원
- 파일 크기 제한 설정 (예: 200MB)

### 3.2 로그 파싱
- EVTX → JSON 변환
- 필수 필드 추출
  - EventID
  - Level
  - TimeCreated
  - Provider
  - Computer
  - Message

### 3.3 로그 조회
- 테이블 기반 리스트 UI
- 컬럼 정렬
- 페이징 / 무한 스크롤

### 3.4 검색 & 필터
- 기간 필터
- 로그 종류(System/Application/Security)
- Event Level
- Event ID
- 키워드(Message)

### 3.5 분석 기능
- Event ID별 발생 빈도
- Error / Critical Top N
- 특정 시간대 집중 발생 이벤트

### 3.6 AI 분석 (선택)
- 로그 요약
- 장애 원인 추정
- 초보자용 설명

### 3.7 결과 내보내기
- CSV / JSON 다운로드
- 분석 요약 리포트
- **웹 분석 보고서 PDF 저장**
  - 화면에 표시된 분석 결과를 PDF로 변환
  - 차트, 통계, 이벤트 리스트 포함
  - 보고서 양식 및 서식 지원

---

## 4. 아키텍처 구조 (Batch + HTTPS + PQC 확장)

### 4.1 전체 구조

```
[Browser]
   │ HTTPS (TLS / Hybrid TLS)
   ▼
[Reverse Proxy / Gateway]
   │
   │ ├─ TLS Termination
   │ ├─ PQC Hybrid Key Exchange
   │ └─ Security Header
   ▼
[Spring Boot Backend]
   │
   ├─ WebFlux Controller
   ├─ EVTX Parsing & Batch Save
   │     └─ flush / clear
   ├─ AI Analysis
   ├─ Query API
   │
   ├─ Redis
   └─ Database
```

---



## 5. 사용 기술 스택 정리 (보안·PQC 포함)

### 5.1 Backend
| 영역 | 기술 |
|---|---|
| Framework | Spring Boot 3.x |
| Web | Spring WebFlux |
| View | Thymeleaf |
| ORM | Spring Data JPA |
| Batch | JPA batch insert + flush/clear |
| Cache | Redis |
| AI | Spring AI |
| PDF | iText / Apache PDFBox / OpenPDF |

### 5.2 Security / Network
| 영역 | 기술 |
|---|---|
| HTTPS | TLS 1.3 |
| Cert | Let’s Encrypt / Internal CA |
| Proxy | Nginx |
| Gateway | Custom / Nginx / Envoy |
| PQC | Hybrid TLS (Kyber 등 PoC) |

---
|---|
| Framework | Spring Boot 3.x |
| Web | Spring WebFlux |
| View | Thymeleaf |
| ORM | Spring Data JPA (Hibernate) |
| Batch 전략 | JPA batch insert + flush/clear |
| Cache | Redis |
| Reactive | Project Reactor |
| AI | Spring AI + OpenAI |
| File | Multipart + Streaming |
| PDF | iText / Apache PDFBox / OpenPDF |

---
|---|
| Framework | Spring Boot 3.x |
| Web | Spring WebFlux |
| View | Thymeleaf |
| Cache | Redis |
| Reactive | Project Reactor |
| AI | Spring AI + OpenAI |
| File | Multipart + Streaming |
| PDF | iText / Apache PDFBox / OpenPDF |

### 5.2 Frontend (서버 렌더링)
| 영역 | 기술 |
|---|---|
| Template | Thymeleaf |
| UI | **1~2개 라이브러리만 사용** |
| | Tailwind CSS (또는 Alpine.js 조합) |
| 디자인 컨셉 | **TOSS 스타일 참고** |
| | 심플하고 현대적인 미니멀 디자인 |
| | 넓은 여백, 명확한 타이포그래피 |
| Chart | Chart.js (필요시) |

#### UI/UX 디자인 원칙
- **라이브러리 최소화**: 1~2개 라이브러리만 사용 (의존성 최소화)
- **TOSS 스타일 디자인**: 
  - 깔끔하고 미니멀한 인터페이스
  - 넓은 여백과 명확한 정보 계층 구조
  - 직관적인 네비게이션
  - 현대적인 타이포그래피와 컬러 시스템
- **사용성 중심**: 비전문가도 쉽게 사용할 수 있는 직관적인 UI

### 5.3 Infra
| 영역 | 기술 |
|---|---|
| Build | Gradle |
| Container | Docker |
| OS | Windows / Linux |

---

## 6. Redis / WebFlux / Spring AI / Batch 전략 도입 포인트

### 6.1 Redis
- 업로드 파일 메타정보
- 파싱 중간 상태 저장 (진행률)
- 검색 조건 캐시

### 6.2 WebFlux
- 대용량 EVTX 파일 스트리밍 처리
- Non-blocking Upload
- boundedElastic()로 Blocking 구간 분리

### 6.3 JPA Batch Flush / Clear (핵심)

#### 적용 위치
- EVTX 파싱 후 Event 엔티티 대량 저장 구간

#### 전략
- 일정 건수(N=500~1000)마다
  - EntityManager.flush()
  - EntityManager.clear()

#### 효과
- Persistence Context 메모리 폭증 방지
- 대량 Insert 성능 개선

#### Hibernate 설정 예시
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc.batch_size: 1000
        order_inserts: true
```

### 6.4 Spring AI
- 저장 완료 후 로그 요약
- Error / Critical 이벤트 설명

---


## 7. 4주 개발 일정 제안 (보안·PQC 확장 포함)

### Week 1
- 프로젝트 세팅 (WebFlux + JPA + Redis)
- EVTX 파서 PoC
- 파일 업로드 + 스트리밍 처리
- JPA batch insert + flush/clear 적용
- 기본 로그 리스트 화면

### Week 2
- 검색 / 필터링
- Redis 캐싱
- AI 로그 요약
- 대용량 파일 성능 테스트
- 분석 보고서 PDF 생성 기능

### Week 3 (HTTPS / 도메인)
- 도메인 확보 및 DNS 설정
- HTTPS 적용 (TLS)
  - 인증서 발급 (Let’s Encrypt 또는 내부 CA)
  - Spring Boot SSL 설정
- Reverse Proxy(Nginx) 연계
- 보안 헤더 설정

### Week 4 (PQC Gateway & Hybrid 실습)
- PQC 개념 검증(PoC)
- PQC 기반 Gateway Bridge 설계
- Hybrid TLS 구성 실습
  - 기존 TLS + PQC Key Exchange
- Gateway ↔ Backend 보안 통신 검증

---



## 8. 확장 아이디어
- Linux 로그(syslog) 지원
- 실시간 에이전트 연동
- 고객사별 로그 히스토리
- 장애 패턴 학습

---

## 9. 한 줄 요약
> **"Windows 이벤트 로그를 사람이 이해할 수 있게 번역해주는 웹 도구"**

