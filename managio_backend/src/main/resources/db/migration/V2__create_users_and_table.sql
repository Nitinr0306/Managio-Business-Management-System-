-- ============================================================
-- V2: Create businesses table
-- ============================================================

CREATE TABLE IF NOT EXISTS businesses (
                                          id           BIGSERIAL PRIMARY KEY,
                                          owner_id     BIGINT       NOT NULL,
                                          name         VARCHAR(200) NOT NULL,
    address      VARCHAR(500),
    phone        VARCHAR(20),
    email        VARCHAR(255),
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    member_count INT          NOT NULL DEFAULT 0,
    staff_count  INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMP,
    version      BIGINT
    );

CREATE INDEX IF NOT EXISTS idx_business_owner  ON businesses (owner_id);
CREATE INDEX IF NOT EXISTS idx_business_status ON businesses (status);