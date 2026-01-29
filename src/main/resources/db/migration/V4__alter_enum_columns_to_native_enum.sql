-- VARCHAR enum 컬럼을 MariaDB ENUM 타입으로 변경 (Hibernate schema-validation 통과)
-- 이미 ENUM으로 생성된 DB라면 이 스크립트는 실패할 수 있음 → 수동 확인 후 필요 시 skip

ALTER TABLE log_files
    MODIFY COLUMN parsing_status ENUM('IN_PROGRESS','COMPLETED','FAILED') NOT NULL DEFAULT 'IN_PROGRESS';

ALTER TABLE events
    MODIFY COLUMN level ENUM('INFORMATION','WARNING','ERROR','CRITICAL') NOT NULL,
    MODIFY COLUMN channel ENUM('SYSTEM','APPLICATION','SECURITY','SETUP','FORWARDED_EVENTS') NOT NULL;
