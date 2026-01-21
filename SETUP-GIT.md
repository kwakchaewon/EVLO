# Git 커밋 메시지 자동 제안 설정 가이드

TASK.md 기반으로 Git 커밋 메시지를 자동으로 제안하는 설정이 완료되었습니다.

## ✅ 설정 완료 내용

1. **커밋 템플릿** (`.gitmessage`)
   - 커밋 시 자동으로 표시되는 템플릿
   - Conventional Commits 형식 가이드 포함

2. **Git Hook** (`.git/hooks/prepare-commit-msg`)
   - 변경된 파일을 자동 분석
   - TASK.md 관련 작업 섹션 제안

3. **제안 스크립트**
   - `scripts/suggest-commit.sh` (Bash/Linux/Mac)
   - `scripts/suggest-commit.ps1` (PowerShell/Windows)

## 🚀 사용 방법

### 방법 1: Git 커밋 시 자동 제안 (권장)

#### Windows (Git Bash 사용)
```bash
# Git Bash 실행 후
git add .
git commit
```

커밋 메시지 편집기가 열리면 자동으로 다음이 표시됩니다:
- 커밋 템플릿
- 변경된 파일 분석 결과
- TASK.md 관련 작업 섹션 제안

#### Windows (PowerShell 사용)
```powershell
# 커밋 전 제안 확인
.\scripts\suggest-commit.ps1

# 그 후 커밋
git add .
git commit
```

### 방법 2: 커밋 전 제안 확인

#### Windows (PowerShell)
```powershell
git add .
.\scripts\suggest-commit.ps1
git commit
```

#### Linux/Mac (Bash)
```bash
git add .
./scripts/suggest-commit.sh
git commit
```

## 📝 커밋 메시지 예시

제안받은 내용을 바탕으로 다음과 같이 커밋하세요:

```bash
git commit -m "feat(parser): EVTX 파싱 서비스 구현

Week 1: EVTX 파서 PoC (2.3)
- EvtxParserService 인터페이스 및 구현
- 스트리밍 파싱 메서드 추가"
```

또는:

```bash
git commit -m "feat(db): Event 엔티티 및 Repository 구현

Week 1: 데이터베이스 설계 (2.2)
- Event 엔티티 설계 완료
- EventRepository 인터페이스 생성"
```

## 🔍 파일 패턴 자동 인식

Git Hook이 자동으로 인식하는 파일 패턴:

| 파일 패턴 | TASK.md 섹션 | 이모지 |
|---------|------------|--------|
| `build.gradle`, `settings.gradle` | 2.1 프로젝트 세팅 | 📦 |
| `application*.yml` | 2.1, 8. 설정 파일 | ⚙️ |
| `*Entity.java`, `entity/*.java` | 2.2 데이터베이스 설계 | 🗄️ |
| `*Repository.java` | 2.2 데이터베이스 설계 | 🗄️ |
| `*Parser*.java`, `*Evtx*.java` | 2.3 EVTX 파서 PoC | 🔍 |
| `*Upload*.java`, `*upload*` | 2.4 파일 업로드 기능 | 📤 |
| `*Service*.java` | 2.5 스트리밍/Batch 저장 | ⚡ |
| `*Controller*.java`, `*Router*.java` | API Controller | 🌐 |
| `templates/*.html`, `*.html` | 2.6 기본 UI | 🎨 |
| `*.css`, `*tailwind*` | 2.6 Tailwind CSS | 🎨 |
| `*Search*.java`, `*Filter*.java` | 3.1 검색 및 필터링 | 🔎 |
| `*Redis*.java`, `*cache*` | 3.2 Redis 캐싱 | 💾 |
| `*Analysis*.java` | 3.3 분석 기능 | 📊 |
| `*AI*.java`, `*SpringAi*` | 3.4 AI 로그 요약 | 🤖 |
| `*PDF*.java`, `*export*` | 3.5 PDF / 3.6 내보내기 | 📄 |
| `*Exception*.java`, `*Error*.java` | 6.1 에러 처리 | ⚠️ |
| `*Test*.java`, `*test*` | 6.3 테스트 | 🧪 |
| `Dockerfile`, `docker-compose*` | 6.4 Docker 설정 | 🐳 |
| `logback*`, `logging*` | 6.2 로깅 | 📝 |

## 💡 작업 흐름

1. **코드 작성** → 파일 변경
2. **스테이징** → `git add .` 또는 `git add <파일>`
3. **커밋** → `git commit`
   - Git Hook이 자동으로 제안 표시
   - 또는 `.\scripts\suggest-commit.ps1` 실행하여 미리 확인
4. **TASK.md 업데이트** → 완료한 작업 항목 체크
   ```markdown
   - [x] 완료한 작업
   - [ ] 아직 안 한 작업
   ```

## ⚠️ 주의사항

### Windows 사용자
- **Git Bash 권장**: Git Hook은 Bash 스크립트로 작성되어 있어 Git Bash에서 가장 잘 작동합니다
- PowerShell에서도 스크립트(`suggest-commit.ps1`)는 작동하지만, Git Hook은 Git Bash에서 실행됩니다

### Git Hook 동작
- Git Hook은 `.git/hooks/` 디렉토리에 있어 버전 관리에 포함되지 않습니다
- 프로젝트를 새로 클론할 경우 Hook 파일을 다시 복사해야 합니다
- Hook 파일이 실행 가능한 권한이 있어야 합니다 (Linux/Mac)

### 설정 확인
```bash
# 커밋 템플릿 설정 확인
git config --local commit.template

# Git Hook 파일 확인
ls -la .git/hooks/prepare-commit-msg
```

## 🔧 문제 해결

### Hook이 작동하지 않는 경우
1. Git Bash를 사용하고 있는지 확인
2. Hook 파일에 실행 권한이 있는지 확인 (Linux/Mac)
3. `.git/hooks/prepare-commit-msg` 파일이 존재하는지 확인

### 커밋 템플릿이 표시되지 않는 경우
```bash
# 설정 확인
git config --local commit.template

# 수동 설정
git config --local commit.template .gitmessage
```

## 📚 참고 자료

- [Conventional Commits](https://www.conventionalcommits.org/)
- [TASK.md](./docs/TASK.md) - 개발 작업 목록
- [Git Hooks 문서](https://git-scm.com/book/en/v2/Customizing-Git-Git-Hooks)
