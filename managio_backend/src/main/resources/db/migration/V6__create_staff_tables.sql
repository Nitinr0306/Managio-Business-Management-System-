-- ============================================================
-- V6: Create staff, staff_invitations, and staff_permissions tables
-- ============================================================

CREATE TABLE IF NOT EXISTS staff (
                                     id                        BIGSERIAL PRIMARY KEY,
                                     business_id               BIGINT         NOT NULL REFERENCES businesses (id),
    user_id                   BIGINT         NOT NULL REFERENCES users (id),
    role                      VARCHAR(50)    NOT NULL,
    status                    VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    hire_date                 DATE           NOT NULL,
    termination_date          DATE,
    department                VARCHAR(100),
    designation               VARCHAR(100),
    salary                    NUMERIC(10, 2),
    employee_id               VARCHAR(20),
    phone                     VARCHAR(20),
    email                     VARCHAR(255),
    address                   VARCHAR(500),
    notes                     TEXT,
    emergency_contact         TEXT,
    can_login                 BOOLEAN        NOT NULL DEFAULT TRUE,
    can_manage_members        BOOLEAN        NOT NULL DEFAULT FALSE,
    can_manage_payments       BOOLEAN        NOT NULL DEFAULT FALSE,
    can_manage_subscriptions  BOOLEAN        NOT NULL DEFAULT FALSE,
    can_view_reports          BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at                TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMP      NOT NULL DEFAULT NOW(),
    deleted_at                TIMESTAMP,
    version                   BIGINT,
    CONSTRAINT uk_staff_business_user UNIQUE (business_id, user_id)
    );

CREATE INDEX IF NOT EXISTS idx_staff_business ON staff (business_id);
CREATE INDEX IF NOT EXISTS idx_staff_user     ON staff (user_id);
CREATE INDEX IF NOT EXISTS idx_staff_status   ON staff (status);
CREATE INDEX IF NOT EXISTS idx_staff_role     ON staff (role);

-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS staff_invitations (
                                                 id                   BIGSERIAL PRIMARY KEY,
                                                 business_id          BIGINT       NOT NULL REFERENCES businesses (id),
    email                VARCHAR(255) NOT NULL,
    role                 VARCHAR(50)  NOT NULL,
    token                VARCHAR(500) NOT NULL UNIQUE,
    expires_at           TIMESTAMP    NOT NULL,
    used                 BOOLEAN      NOT NULL DEFAULT FALSE,
    used_at              TIMESTAMP,
    accepted_by_user_id  BIGINT       REFERENCES users (id),
    invited_by           BIGINT       NOT NULL REFERENCES users (id),
    message              VARCHAR(500),
    department           VARCHAR(100),
    designation          VARCHAR(100),
    inviter_ip_address   VARCHAR(45),
    inviter_user_agent   VARCHAR(255),
    acceptor_ip_address  VARCHAR(45),
    acceptor_user_agent  VARCHAR(255),
    created_at           TIMESTAMP    NOT NULL DEFAULT NOW(),
    version              BIGINT
    );

CREATE INDEX IF NOT EXISTS idx_invitation_business ON staff_invitations (business_id);
CREATE INDEX IF NOT EXISTS idx_invitation_email    ON staff_invitations (email);
CREATE INDEX IF NOT EXISTS idx_invitation_token    ON staff_invitations (token);
CREATE INDEX IF NOT EXISTS idx_invitation_expires  ON staff_invitations (expires_at);

-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS staff_permissions (
                                                 id          BIGSERIAL PRIMARY KEY,
                                                 staff_id    BIGINT      NOT NULL REFERENCES staff (id) ON DELETE CASCADE,
    permission  VARCHAR(100) NOT NULL,
    granted     BOOLEAN      NOT NULL DEFAULT TRUE,
    notes       VARCHAR(500),
    granted_by  BIGINT       NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    version     BIGINT,
    CONSTRAINT uk_staff_permission UNIQUE (staff_id, permission)
    );

CREATE INDEX IF NOT EXISTS idx_staff_perm_staff      ON staff_permissions (staff_id);
CREATE INDEX IF NOT EXISTS idx_staff_perm_permission ON staff_permissions (permission);