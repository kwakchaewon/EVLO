# TASK.md 기반 커밋 메시지 제안 스크립트 (PowerShell)
# 사용법: .\scripts\suggest-commit.ps1

Write-Host "🔍 변경된 파일 분석 중..." -ForegroundColor Cyan

$changedFiles = git diff --cached --name-only --diff-filter=ACMR

if (-not $changedFiles) {
    Write-Host "⚠️ 스테이징된 변경사항이 없습니다." -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "📝 변경된 파일:" -ForegroundColor Cyan
$changedFiles | ForEach-Object { Write-Host "  - $_" }
Write-Host ""

$taskFile = "docs/TASK.md"

if (-not (Test-Path $taskFile)) {
    Write-Host "⚠️ TASK.md 파일을 찾을 수 없습니다." -ForegroundColor Yellow
    exit 1
}

Write-Host "📋 TASK.md 관련 작업 추천:" -ForegroundColor Green
Write-Host ""

$sections = @{}

foreach ($file in $changedFiles) {
    $matched = $false
    switch -Wildcard ($file) {
        "*build.gradle*" { $sections["프로젝트 세팅"] = "2.1 프로젝트 세팅"; $matched = $true }
        "*settings.gradle*" { $sections["프로젝트 세팅"] = "2.1 프로젝트 세팅"; $matched = $true }
        "*gradle.properties*" { $sections["프로젝트 세팅"] = "2.1 프로젝트 세팅"; $matched = $true }
        "*application*.yml*" { $sections["설정 파일"] = "2.1 프로젝트 세팅 / 8. 설정 파일"; $matched = $true }
        "*application*.yaml*" { $sections["설정 파일"] = "2.1 프로젝트 세팅 / 8. 설정 파일"; $matched = $true }
        "*Entity.java" { $sections["데이터베이스"] = "2.2 데이터베이스 설계"; $matched = $true }
        "*entity\*.java" { $sections["데이터베이스"] = "2.2 데이터베이스 설계"; $matched = $true }
        "*Repository.java" { $sections["Repository"] = "2.2 데이터베이스 설계"; $matched = $true }
        "*repository\*.java" { $sections["Repository"] = "2.2 데이터베이스 설계"; $matched = $true }
        "*Parser*.java" { $sections["EVTX 파서"] = "2.3 EVTX 파서 PoC"; $matched = $true }
        "*parser\*.java" { $sections["EVTX 파서"] = "2.3 EVTX 파서 PoC"; $matched = $true }
        "*Evtx*.java" { $sections["EVTX 파서"] = "2.3 EVTX 파서 PoC"; $matched = $true }
        "*Upload*.java" { $sections["파일 업로드"] = "2.4 파일 업로드 기능"; $matched = $true }
        "*upload*" { $sections["파일 업로드"] = "2.4 파일 업로드 기능"; $matched = $true }
        "*FileUpload*.java" { $sections["파일 업로드"] = "2.4 파일 업로드 기능"; $matched = $true }
        "*Service*.java" { $sections["서비스"] = "2.5 스트리밍 처리 및 Batch 저장"; $matched = $true }
        "*service\*.java" { $sections["서비스"] = "2.5 스트리밍 처리 및 Batch 저장"; $matched = $true }
        "*Controller*.java" { $sections["API"] = "API Controller (관련 섹션 확인 필요)"; $matched = $true }
        "*controller\*.java" { $sections["API"] = "API Controller (관련 섹션 확인 필요)"; $matched = $true }
        "*Router*.java" { $sections["API"] = "API Controller (관련 섹션 확인 필요)"; $matched = $true }
        "*templates\*.html" { $sections["UI"] = "2.6 기본 UI (Thymeleaf)"; $matched = $true }
        "*.html" { $sections["UI"] = "2.6 기본 UI (Thymeleaf)"; $matched = $true }
        "*static\*.css" { $sections["CSS"] = "2.6 기본 UI / Tailwind CSS"; $matched = $true }
        "*.css" { $sections["CSS"] = "2.6 기본 UI / Tailwind CSS"; $matched = $true }
        "*tailwind*" { $sections["CSS"] = "2.6 기본 UI / Tailwind CSS"; $matched = $true }
        "*Search*.java" { $sections["검색/필터"] = "3.1 검색 및 필터링"; $matched = $true }
        "*Filter*.java" { $sections["검색/필터"] = "3.1 검색 및 필터링"; $matched = $true }
        "*search*" { $sections["검색/필터"] = "3.1 검색 및 필터링"; $matched = $true }
        "*filter*" { $sections["검색/필터"] = "3.1 검색 및 필터링"; $matched = $true }
        "*Redis*.java" { $sections["Redis"] = "3.2 Redis 캐싱"; $matched = $true }
        "*redis*" { $sections["Redis"] = "3.2 Redis 캐싱"; $matched = $true }
        "*cache*" { $sections["Redis"] = "3.2 Redis 캐싱"; $matched = $true }
        "*Analysis*.java" { $sections["분석"] = "3.3 분석 기능"; $matched = $true }
        "*analysis*" { $sections["분석"] = "3.3 분석 기능"; $matched = $true }
        "*statistics*" { $sections["분석"] = "3.3 분석 기능"; $matched = $true }
        "*AI*.java" { $sections["AI"] = "3.4 AI 로그 요약"; $matched = $true }
        "*ai*" { $sections["AI"] = "3.4 AI 로그 요약"; $matched = $true }
        "*SpringAi*" { $sections["AI"] = "3.4 AI 로그 요약"; $matched = $true }
        "*PDF*.java" { $sections["PDF/내보내기"] = "3.5 PDF 생성 기능 / 3.6 내보내기 기능"; $matched = $true }
        "*pdf*" { $sections["PDF/내보내기"] = "3.5 PDF 생성 기능 / 3.6 내보내기 기능"; $matched = $true }
        "*export*" { $sections["PDF/내보내기"] = "3.5 PDF 생성 기능 / 3.6 내보내기 기능"; $matched = $true }
        "*Exception*.java" { $sections["에러 처리"] = "6.1 에러 처리"; $matched = $true }
        "*exception*" { $sections["에러 처리"] = "6.1 에러 처리"; $matched = $true }
        "*Error*.java" { $sections["에러 처리"] = "6.1 에러 처리"; $matched = $true }
        "*Test*.java" { $sections["테스트"] = "6.3 테스트"; $matched = $true }
        "*test*" { $sections["테스트"] = "6.3 테스트"; $matched = $true }
        "*Tests.java" { $sections["테스트"] = "6.3 테스트"; $matched = $true }
        "*Dockerfile*" { $sections["Docker"] = "6.4 Docker 설정"; $matched = $true }
        "*docker-compose*" { $sections["Docker"] = "6.4 Docker 설정"; $matched = $true }
        "*.dockerignore" { $sections["Docker"] = "6.4 Docker 설정"; $matched = $true }
        "*logback*" { $sections["로깅"] = "6.2 로깅"; $matched = $true }
        "*logging*" { $sections["로깅"] = "6.2 로깅"; $matched = $true }
    }
}

# 추천 섹션 출력
foreach ($key in $sections.Keys | Sort-Object) {
    Write-Host "  ✅ $key : $($sections[$key])" -ForegroundColor Green
}

Write-Host ""
Write-Host "💡 TASK.md에서 해당 섹션을 확인하세요: docs/TASK.md" -ForegroundColor Cyan
Write-Host ""
Write-Host "📝 커밋 메시지 예시:" -ForegroundColor Yellow
Write-Host ""

# 커밋 타입 추천
if ($sections.ContainsKey("테스트")) {
    Write-Host "  test: 테스트 코드 추가" -ForegroundColor White
} elseif ($sections.ContainsKey("UI") -or $sections.ContainsKey("CSS")) {
    Write-Host "  feat(ui): UI 컴포넌트 구현" -ForegroundColor White
} elseif ($sections.ContainsKey("API")) {
    Write-Host "  feat(api): API 엔드포인트 구현" -ForegroundColor White
} elseif ($sections.ContainsKey("데이터베이스") -or $sections.ContainsKey("Repository")) {
    Write-Host "  feat(db): 데이터베이스 설계 및 Repository 구현" -ForegroundColor White
} elseif ($sections.ContainsKey("EVTX 파서")) {
    Write-Host "  feat(parser): EVTX 파서 구현" -ForegroundColor White
} else {
    Write-Host "  feat: 기능 구현" -ForegroundColor White
}

Write-Host ""
