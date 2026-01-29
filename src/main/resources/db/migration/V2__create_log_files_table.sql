-- 업로드된 EVTX 파일 메타정보
CREATE TABLE log_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    filename VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    parsing_status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    uploaded_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    INDEX idx_log_files_parsing_status (parsing_status),
    INDEX idx_log_files_uploaded_at (uploaded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
