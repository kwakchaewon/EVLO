# TODO – EVTX 파싱 (python-evtx 기반 외부 서비스 연동)

## 목표
- python-evtx를 별도 서비스/프로세스로 띄워 EVTX → JSON 변환 후 Spring Boot에서 소비
- 자바 코드에서는 HTTP/프로세스 호출로 결과만 수신 (JNI 불필요)

## 아키텍처 옵션
- **옵션 A: REST 마이크로서비스**  
  - 컨테이너: `python:3.x` 기반, `python-evtx` 설치  
  - API: `POST /parse` (multipart/form-data 또는 file path), 응답은 JSON 이벤트 리스트
- **옵션 B: CLI 프로세스 호출**  
  - 자바에서 로컬 명령 호출 → stdout JSON 수신  
  - 단일 인스턴스 배포/운영 시 간단하지만, 확장성과 모니터링은 REST 대비 제한

## API 설계(권장: REST)
- `POST /parse`
  - Body: `file` (EVTX 업로드) 또는 `filePath` (공유 볼륨 경로)
  - Query: `maxEvents`, `offset` 등 선택 파라미터
  - Response (JSON):
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
          "message": "...",
          "rawXml": "<Event>...</Event>"
        }
      ],
      "count": 123
    }
    ```

## Spring Boot 연동 작업
- [x] `python-evtx` 서비스 엔드포인트/토큰을 `application.yml`에 설정 (`evtx.service.url`, `evtx.service.timeoutMs`)
- [x] `EvtxParserService`를 HTTP 클라이언트 호출 방식으로 교체 (WebClient 권장)
  - 업로드한 임시 파일을 multipart로 evtx-service `POST /parse` 전달 → JSON 수신 → `Event` 매핑
- [x] 실패/타임아웃/재시도 정책 정의 (5xx만 재시도, 타임아웃·retry 설정)
- [x] 업로드 파일 삭제 타이밍 재점검 (파서 호출 후 `FileUploadService` finally에서 삭제)

## python-evtx 서비스 구현(요약)
- [ ] `pip install python-evtx flask` (또는 FastAPI)
- [ ] `/parse` 구현: 업로드 파일 저장 → python-evtx로 파싱 → JSON 직렬화 후 응답
- [ ] Dockerfile 작성
  - 베이스: `python:3.x-slim`
  - 포트: 8081 (예시)
  - 헬스체크: `/healthz`
- [ ] 로깅/에러 핸들링: 파싱 실패 시 명확한 메시지와 HTTP 4xx/5xx 구분

## 배포/운영
- [ ] Docker Compose/K8s에서 Spring Boot와 python-evtx를 같은 네트워크에 배치
- [ ] 공유 볼륨 경로(옵션): 업로드 파일을 공유 후 경로만 전달하면 대용량 업로드 시 네트워크 복사 최소화
- [ ] 리소스 제한: python-evtx 컨테이너 CPU/메모리 리밋 설정
- [ ] 보안: 내부 네트워크 전용, 필요 시 mTLS/토큰

## 테스트
- [ ] 샘플 EVTX 파일로 E2E 테스트 (업로드 → 파싱 → 이벤트 저장)
- [ ] 대용량/오류 파일 케이스(손상된 EVTX) 검증
- [ ] 성능 측정: 파일 크기별 처리 시간, 이벤트 수 대비 처리량

## 백로그 / 추후 개선
- [ ] 메시지 템플릿 해석(Provider DLL)까지 확장할지 검토
- [ ] 스트리밍 파싱 지원 여부 검토
- [ ] 파싱 결과 캐싱 전략 연계 (Redis) 검토
