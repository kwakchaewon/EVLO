#!/bin/bash
#
# TASK.md 기반 커밋 메시지 제안 스크립트
# 사용법: ./scripts/suggest-commit.sh
#

echo "🔍 변경된 파일 분석 중..."
CHANGED_FILES=$(git diff --cached --name-only --diff-filter=ACMR)

if [ -z "$CHANGED_FILES" ]; then
    echo "⚠️ 스테이징된 변경사항이 없습니다."
    exit 1
fi

echo ""
echo "📝 변경된 파일:"
echo "$CHANGED_FILES"
echo ""

TASK_FILE="docs/TASK.md"

if [ ! -f "$TASK_FILE" ]; then
    echo "⚠️ TASK.md 파일을 찾을 수 없습니다."
    exit 1
fi

echo "📋 TASK.md 관련 작업 추천:"
echo ""

# 파일 패턴별 작업 섹션 출력
declare -A SECTIONS

for file in $CHANGED_FILES; do
    case "$file" in
        *build.gradle*|*settings.gradle*|*gradle.properties*)
            SECTIONS["프로젝트 세팅"]="2.1 프로젝트 세팅"
            ;;
        *application*.yml*|*application*.yaml*|*application*.properties*)
            SECTIONS["설정 파일"]="2.1 프로젝트 세팅 / 8. 설정 파일"
            ;;
        *Entity.java|*entity/*.java)
            SECTIONS["데이터베이스"]="2.2 데이터베이스 설계"
            ;;
        *Repository.java|*repository/*.java)
            SECTIONS["Repository"]="2.2 데이터베이스 설계"
            ;;
        *Parser*.java|*parser/*.java|*Evtx*.java)
            SECTIONS["EVTX 파서"]="2.3 EVTX 파서 PoC"
            ;;
        *Upload*.java|*upload*|*FileUpload*.java)
            SECTIONS["파일 업로드"]="2.4 파일 업로드 기능"
            ;;
        *Service*.java|*service/*.java)
            SECTIONS["서비스"]="2.5 스트리밍 처리 및 Batch 저장"
            ;;
        *Controller*.java|*controller/*.java|*Router*.java)
            SECTIONS["API"]="API Controller (관련 섹션 확인 필요)"
            ;;
        *templates/*.html|*.html)
            SECTIONS["UI"]="2.6 기본 UI (Thymeleaf)"
            ;;
        *static/*.css|*.css|*tailwind*)
            SECTIONS["CSS"]="2.6 기본 UI / Tailwind CSS"
            ;;
        *Search*.java|*Filter*.java|*search*|*filter*)
            SECTIONS["검색/필터"]="3.1 검색 및 필터링"
            ;;
        *Redis*.java|*redis*|*cache*)
            SECTIONS["Redis"]="3.2 Redis 캐싱"
            ;;
        *Analysis*.java|*analysis*|*statistics*)
            SECTIONS["분석"]="3.3 분석 기능"
            ;;
        *AI*.java|*ai*|*SpringAi*)
            SECTIONS["AI"]="3.4 AI 로그 요약"
            ;;
        *PDF*.java|*pdf*|*export*)
            SECTIONS["PDF/내보내기"]="3.5 PDF 생성 기능 / 3.6 내보내기 기능"
            ;;
        *Exception*.java|*exception*|*Error*.java)
            SECTIONS["에러 처리"]="6.1 에러 처리"
            ;;
        *Test*.java|*test*|*Tests.java)
            SECTIONS["테스트"]="6.3 테스트"
            ;;
        *Dockerfile*|*docker-compose*|*.dockerignore)
            SECTIONS["Docker"]="6.4 Docker 설정"
            ;;
        *logback*|*logging*)
            SECTIONS["로깅"]="6.2 로깅"
            ;;
    esac
done

# 추천 섹션 출력
for key in "${!SECTIONS[@]}"; do
    echo "  ✅ $key: ${SECTIONS[$key]}"
done

echo ""
echo "💡 TASK.md에서 해당 섹션을 확인하세요: docs/TASK.md"
echo ""
echo "📝 커밋 메시지 예시:"
echo ""

# 커밋 타입 추천
if [ -n "${SECTIONS[테스트]}" ]; then
    echo "  test: 테스트 코드 추가"
elif [ -n "${SECTIONS[UI]}" ] || [ -n "${SECTIONS[CSS]}" ]; then
    echo "  feat(ui): UI 컴포넌트 구현"
elif [ -n "${SECTIONS[API]}" ]; then
    echo "  feat(api): API 엔드포인트 구현"
elif [ -n "${SECTIONS[데이터베이스]}" ] || [ -n "${SECTIONS[Repository]}" ]; then
    echo "  feat(db): 데이터베이스 설계 및 Repository 구현"
elif [ -n "${SECTIONS[EVTX 파서]}" ]; then
    echo "  feat(parser): EVTX 파서 구현"
else
    echo "  feat: 기능 구현"
fi

echo ""
