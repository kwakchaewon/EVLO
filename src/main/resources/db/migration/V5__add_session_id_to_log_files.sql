-- 비회원 세션 구분 (업로드·조회 시 "이번 세션"만 보이도록, 추후 회원이면 user_id 사용 예정)
ALTER TABLE log_files ADD COLUMN session_id VARCHAR(36) NULL AFTER created_at;
CREATE INDEX idx_log_files_session_id ON log_files (session_id);
