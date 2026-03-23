-- Add stable, human-readable alphanumeric identifiers for audit records.

ALTER TABLE auth_audit_logs
    ADD COLUMN IF NOT EXISTS event_id VARCHAR(20);

UPDATE auth_audit_logs
SET event_id = 'A' || UPPER(SUBSTRING(MD5(id::text || clock_timestamp()::text || random()::text), 1, 11))
WHERE event_id IS NULL OR event_id = '';

ALTER TABLE auth_audit_logs
    ALTER COLUMN event_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_auth_audit_logs_event_id
    ON auth_audit_logs (event_id);


ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS log_id VARCHAR(20);

UPDATE audit_logs
SET log_id = 'L' || UPPER(SUBSTRING(MD5(id::text || clock_timestamp()::text || random()::text), 1, 11))
WHERE log_id IS NULL OR log_id = '';

ALTER TABLE audit_logs
    ALTER COLUMN log_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_audit_logs_log_id
    ON audit_logs (log_id);
