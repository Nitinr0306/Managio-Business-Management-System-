-- ============================================================
-- V7: Create business audit_logs table
-- ============================================================

CREATE TABLE IF NOT EXISTS audit_logs (
                                          id          BIGSERIAL PRIMARY KEY,
                                          business_id BIGINT       NOT NULL,
                                          user_id     BIGINT       NOT NULL,
                                          action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50)  NOT NULL,
    entity_id   BIGINT,
    details     TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_audit_business ON audit_logs (business_id);
CREATE INDEX IF NOT EXISTS idx_audit_user     ON audit_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_created  ON audit_logs (created_at);