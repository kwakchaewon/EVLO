CREATE TABLE events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    level VARCHAR(20) NOT NULL,
    time_created DATETIME NOT NULL,
    provider VARCHAR(500),
    computer VARCHAR(255),
    message TEXT,
    channel VARCHAR(50) NOT NULL,
    log_file_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (log_file_id) REFERENCES log_files(id) ON DELETE CASCADE,
    INDEX idx_time_created (time_created),
    INDEX idx_event_id (event_id),
    INDEX idx_level (level),
    INDEX idx_channel (channel),
    INDEX idx_log_file_id (log_file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
