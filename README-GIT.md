# Git 커밋 메시지 자동 제안 설정

TASK.md 기반으로 Git 커밋 메시지를 자동으로 제안하는 설정이 완료되었습니다.

## 설정 내용

### 1. 커밋 템플릿 설정
- `.gitmessage` 파일 생성
- Git 커밋 시 자동으로 템플릿이 표시됩니다

### 2. Git Hook 설정
- `.git/hooks/prepare-commit-msg` 생성
- 커밋 시 변경된 파일을 분석하여 TASK.md 관련 작업 섹션을 자동 제안

### 3. 커밋 메시지 제안 스크립트
- `scripts/suggest-commit.sh` 생성
- 커밋 전에 수동으로 실행하여 제안 확인 가능

## 사용 방법

### 방법 1: Git 커밋 시 자동 제안 (권장)
```bash
# 변경사항 스테이징
git add .

# 커밋 시 자동으로 제안이 표시됩니다
git commit
```

커밋 메시지 편집기가 열리면 자동으로 TASK.md 관련 작업 제안이 포함됩니다.

### 방법 2: 커밋 전 제안 확인
```bash
# 변경사항 스테이징
git add .

# 제안 확인
./scripts/suggest-commit.sh

# 그 후 커밋
git commit
```

### 방법 3: 커밋 메시지 타입 가이드
커밋 메시지는 Conventional Commits 형식을 따릅니다:

```
<type>(<scope>): <subject>

<body>
```

**타입:**
- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `docs`: 문서 수정
- `style`: 코드 포맷팅
- `refactor`: 코드 리팩토링
- `test`: 테스트 코드
- `chore`: 빌드/설정 관련
- `perf`: 성능 개선

**예시:**
```bash
git commit -m "feat(parser): EVTX 파싱 서비스 구현

Week 1: EVTX 파서 PoC (2.3)
- EvtxParserService 인터페이스 및 구현
- 스트리밍 파싱 메서드 추가"
```

## 파일 패턴 매핑

Git Hook은 다음 파일 패턴을 감지하여 관련 작업을 제안합니다:

| 파일 패턴 | 관련 섹션 |
|---------|---------|
| `*build.gradle*`, `*settings.gradle*` | 2.1 프로젝트 세팅 |
| `*application*.yml*` | 2.1 프로젝트 세팅 / 8. 설정 파일 |
| `*Entity.java`, `*entity/*.java` | 2.2 데이터베이스 설계 |
| `*Repository.java` | 2.2 데이터베이스 설계 |
| `*Parser*.java`, `*Evtx*.java` | 2.3 EVTX 파서 PoC |
| `*Upload*.java`, `*upload*` | 2.4 파일 업로드 기능 |
| `*Service*.java` | 2.5 스트리밍 처리 및 Batch 저장 |
| `*Controller*.java`, `*Router*.java` | API Controller |
| `*templates/*.html` | 2.6 기본 UI (Thymeleaf) |
| `*static/*.css`, `*tailwind*` | 2.6 기본 UI / Tailwind CSS |
| `*Search*.java`, `*Filter*.java` | 3.1 검색 및 필터링 |
| `*Redis*.java`, `*cache*` | 3.2 Redis 캐싱 |
| `*Analysis*.java` | 3.3 분석 기능 |
| `*AI*.java`, `*SpringAi*` | 3.4 AI 로그 요약 |
| `*PDF*.java`, `*export*` | 3.5 PDF 생성 / 3.6 내보내기 |
| `*Exception*.java`, `*Error*.java` | 6.1 에러 처리 |
| `*Test*.java`, `*test*` | 6.3 테스트 |
| `*Dockerfile*`, `*docker-compose*` | 6.4 Docker 설정 |
| `*logback*`, `*logging*` | 6.2 로깅 |

## TASK.md 작업 체크리스트

커밋 후 TASK.md에서 해당 작업 항목을 체크하세요:

1. `docs/TASK.md` 파일 열기
2. 관련 섹션 찾기
3. 완료한 작업의 체크박스 `[ ]`를 `[x]`로 변경
4. 변경사항 커밋

## 주의사항

- Git Hook은 `.git/hooks/` 디렉토리에 있습니다 (버전 관리 제외)
- 새로운 프로젝트를 클론할 경우 Hook 파일을 다시 설정해야 합니다
- Windows에서는 Git Bash를 사용하는 것을 권장합니다
