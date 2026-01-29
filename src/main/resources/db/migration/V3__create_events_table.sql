-- 파싱된 이벤트 로그 (log_files 1:N) (이미 있으면 스킵)
CREATE TABLE IF NOT EXISTS events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    level ENUM('INFORMATION','WARNING','ERROR','CRITICAL') NOT NULL,
    time_created DATETIME(6) NOT NULL,
    provider VARCHAR(500),
    computer VARCHAR(255),
    message TEXT,
    channel ENUM('SYSTEM','APPLICATION','SECURITY','SETUP','FORWARDED_EVENTS') NOT NULL,
    log_file_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_events_log_file FOREIGN KEY (log_file_id) REFERENCES log_files(id) ON DELETE CASCADE,
    INDEX idx_events_time_created (time_created),
    INDEX idx_events_event_id (event_id),
    INDEX idx_events_level (level),
    INDEX idx_events_channel (channel),
    INDEX idx_events_log_file_id (log_file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
