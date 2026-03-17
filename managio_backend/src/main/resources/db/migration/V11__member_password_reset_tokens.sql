-- ============================================================
-- V11: member_password_reset_tokens
--
-- Members use phone-or-email login (no User account).
-- Password reset tokens for members must be stored separately
-- from user reset tokens so that:
--   1. Token lookup does not collide across the two namespaces
--   2. Cleanup jobs only need to handle one table per domain
--
-- The token flow is:
--   POST /api/v1/members/auth/forgot-password?identifier={phone|email}
--     → generates token, stores it here, sends email
--   POST /api/v1/members/auth/reset-password?token={uuid}&newPassword={pass}
--     → finds token, validates, updates member.password, revokes member refresh tokens
-- ============================================================

CREATE TABLE IF NOT EXISTS member_password_reset_tokens (
                                                            id                   BIGSERIAL    PRIMARY KEY,
                                                            token                VARCHAR(500) NOT NULL UNIQUE,
    member_id            BIGINT       NOT NULL REFERENCES members (id) ON DELETE CASCADE,
    expires_at           TIMESTAMP    NOT NULL,
    used                 BOOLEAN      NOT NULL DEFAULT FALSE,
    used_at              TIMESTAMP,
    request_ip_address   VARCHAR(45),
    request_user_agent   VARCHAR(255),
    reset_ip_address     VARCHAR(45),
    reset_user_agent     VARCHAR(255),
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    version              BIGINT
    );

CREATE INDEX IF NOT EXISTS idx_member_pwd_reset_token
    ON member_password_reset_tokens (token);

CREATE INDEX IF NOT EXISTS idx_member_pwd_reset_member_id
    ON member_password_reset_tokens (member_id);

CREATE INDEX IF NOT EXISTS idx_member_pwd_reset_expires
    ON member_password_reset_tokens (expires_at);