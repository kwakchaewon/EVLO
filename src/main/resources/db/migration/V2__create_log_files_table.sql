CREATE TABLE log_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    filename VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    parsing_status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    uploaded_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_parsing_status (parsing_status),
    INDEX idx_uploaded_at (uploaded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
