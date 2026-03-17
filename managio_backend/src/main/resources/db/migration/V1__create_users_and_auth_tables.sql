-- ============================================================
-- V1: Create users and authentication tables
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
                                     id                   BIGSERIAL PRIMARY KEY,
                                     email                VARCHAR(255) NOT NULL UNIQUE,
    password             VARCHAR(255) NOT NULL,
    first_name           VARCHAR(100),
    last_name            VARCHAR(100),
    phone_number         VARCHAR(20),
    email_verified       BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled              BOOLEAN      NOT NULL DEFAULT TRUE,
    account_locked       BOOLEAN      NOT NULL DEFAULT FALSE,
    account_status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    failed_login_attempts INT         NOT NULL DEFAULT 0,
    last_login_at        TIMESTAMP,
    locked_at            TIMESTAMP,
    password_changed_at  TIMESTAMP,
    profile_image_url    VARCHAR(500),
    preferred_language   VARCHAR(10),
    timezone             VARCHAR(50),
    two_factor_enabled   BOOLEAN      NOT NULL DEFAULT FALSE,
    two_factor_secret    VARCHAR(100),
    metadata             TEXT,
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    deleted_at           TIMESTAMP,
    version              BIGINT
    );

CREATE INDEX IF NOT EXISTS idx_user_email         ON users (email);
CREATE INDEX IF NOT EXISTS idx_user_status        ON users (account_status);
CREATE INDEX IF NOT EXISTS idx_user_created       ON users (created_at);

-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role    VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role)
    );

-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id                BIGSERIAL PRIMARY KEY,
                                              token             VARCHAR(500) NOT NULL UNIQUE,
    user_id           BIGINT       NOT NULL,
    expires_at        TIMESTAMP    NOT NULL,
    revoked           BOOLEAN      NOT NULL DEFAULT FALSE,
    used              BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at        TIMESTAMP,
    used_at           TIMESTAMP,
    replaced_by_token VARCHAR(500),
    device_id         VARCHAR(100),
    user_agent        VARCHAR(255),
    ip_address        VARCHAR(45),
    location          VARCHAR(100),
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    rotation_count    INT          NOT NULL DEFAULT 0,
    last_rotated_at   TIMESTAMP,
    version           BIGINT
    );

CREATE INDEX IF NOT EXISTS idx_refresh_token   ON refresh_tokens (token);
CREATE INDEX IF NOT EXISTS idx_refresh_user_id ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_expires ON refresh_tokens (expires_at);
CREATE INDEX IF NOT EXISTS idx_refresh_revoked ON refresh_tokens (revoked);

-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS email_verification_tokens (
                                                         id                   BIGSERIAL PRIMARY KEY,
                                                         token                VARCHAR(500) NOT NULL UNIQUE,
    user_id              BIGINT       NOT NULL,
    expires_at           TIMESTAMP    NOT NULL,
    used                 BOOLEAN      NOT NULL DEFAULT FALSE,
    used_at              TIMESTAMP,
    request_ip_address   VARCHAR(45),
    request_user_agent   VARCHAR(255),
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    version              BIGINT
    );

CREATE INDEX IF NOT EXISTS idx_email_token   ON email_verification_tokens (token);
CREATE INDEX IF NOT EXISTS idx_email_user_id ON email_verification_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_email_expires ON email_verification_tokens (expires_at);

-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS password_reset_tokens (
                                                     id                   BIGSERIAL PRIMARY KEY,
                                                     token                VARCHAR(500) NOT NULL UNIQUE,
    user_id              BIGINT       NOT NULL,
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

CREATE INDEX IF NOT EXISTS idx_pwd_reset_token   ON password_reset_tokens (token);
CREATE INDEX IF NOT EXISTS idx_pwd_reset_user_id ON password_reset_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_pwd_reset_expires ON password_reset_tokens (expires_at);

-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS auth_audit_logs (
                                               id          BIGSERIAL PRIMARY KEY,
                                               user_id     BIGINT,
                                               email       VARCHAR(255),
    event_type  VARCHAR(50)  NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(500),
    device_id   VARCHAR(100),
    location    VARCHAR(100),
    details     TEXT,
    error_message TEXT,
    request_id  VARCHAR(100),
    session_id  VARCHAR(100),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_auth_audit_user_id ON auth_audit_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_auth_audit_event   ON auth_audit_logs (event_type);
CREATE INDEX IF NOT EXISTS idx_auth_audit_created ON auth_audit_logs (created_at);
CREATE INDEX IF NOT EXISTS idx_auth_audit_status  ON auth_audit_logs (status);
CREATE INDEX IF NOT EXISTS idx_auth_audit_ip      ON auth_audit_logs (ip_address);