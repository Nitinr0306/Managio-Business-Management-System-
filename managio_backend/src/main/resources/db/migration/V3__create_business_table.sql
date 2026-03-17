-- ============================================================
-- V3: Create members table
-- ============================================================

CREATE TABLE IF NOT EXISTS members (
                                       id              BIGSERIAL PRIMARY KEY,
                                       business_id     BIGINT       NOT NULL REFERENCES businesses (id),
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    email           VARCHAR(255),
    date_of_birth   DATE,
    gender          VARCHAR(10),
    address         VARCHAR(500),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    notes           TEXT,
    password        VARCHAR(255),
    account_enabled BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP,
    version         BIGINT
    );

CREATE INDEX IF NOT EXISTS idx_member_business ON members (business_id);
CREATE INDEX IF NOT EXISTS idx_member_name     ON members (first_name, last_name);
CREATE INDEX IF NOT EXISTS idx_member_phone    ON members (phone);
CREATE INDEX IF NOT EXISTS idx_member_status   ON members (status);