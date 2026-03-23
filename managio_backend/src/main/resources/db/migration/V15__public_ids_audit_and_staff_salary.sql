-- ============================================================
-- V15: Public IDs, audit metadata, member auth hardening, salary ledger
-- ============================================================

-- -----------------------------
-- Public IDs for core entities
-- -----------------------------
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS public_id VARCHAR(20);

UPDATE users
SET public_id = 'OWN-' || UPPER(SUBSTRING(MD5(id::text || clock_timestamp()::text || random()::text), 1, 8))
WHERE public_id IS NULL OR public_id = '';

ALTER TABLE users
    ALTER COLUMN public_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_public_id ON users (public_id);


ALTER TABLE businesses
    ADD COLUMN IF NOT EXISTS public_id VARCHAR(20);

UPDATE businesses
SET public_id = LPAD((FLOOR(RANDOM() * 10000))::INT::TEXT, 4, '0')
    || CHR(65 + (FLOOR(RANDOM() * 26))::INT)
    || CHR(65 + (FLOOR(RANDOM() * 26))::INT)
    || CHR(65 + (FLOOR(RANDOM() * 26))::INT)
    || CHR(65 + (FLOOR(RANDOM() * 26))::INT)
WHERE public_id IS NULL
   OR public_id = ''
    OR public_id !~ '^[0-9]{4}[A-Z]{4}$';

ALTER TABLE businesses
    ALTER COLUMN public_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_businesses_public_id ON businesses (public_id);


ALTER TABLE staff
    ADD COLUMN IF NOT EXISTS public_id VARCHAR(20);

UPDATE staff
SET public_id = 'STF-' || UPPER(SUBSTRING(MD5(id::text || clock_timestamp()::text || random()::text), 1, 8))
WHERE public_id IS NULL OR public_id = '';

ALTER TABLE staff
    ALTER COLUMN public_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_staff_public_id ON staff (public_id);


ALTER TABLE members
    ADD COLUMN IF NOT EXISTS public_id VARCHAR(20),
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP;

UPDATE members
SET public_id = 'MBR-' || UPPER(SUBSTRING(MD5(id::text || clock_timestamp()::text || random()::text), 1, 8))
WHERE public_id IS NULL OR public_id = '';

ALTER TABLE members
    ALTER COLUMN public_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_members_public_id ON members (public_id);


ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS public_id VARCHAR(20);

UPDATE payments
SET public_id = 'PAY-' || UPPER(SUBSTRING(MD5(id::text || clock_timestamp()::text || random()::text), 1, 8))
WHERE public_id IS NULL OR public_id = '';

ALTER TABLE payments
    ALTER COLUMN public_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_payments_public_id ON payments (public_id);


-- -----------------------------
-- Audit metadata for every action
-- -----------------------------
ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS actor_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS actor_public_id VARCHAR(20),
    ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45),
    ADD COLUMN IF NOT EXISTS user_agent VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_audit_actor_type ON audit_logs (actor_type);
CREATE INDEX IF NOT EXISTS idx_audit_actor_public_id ON audit_logs (actor_public_id);


-- -----------------------------
-- Member email verification token table
-- -----------------------------
CREATE TABLE IF NOT EXISTS member_email_verification_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    member_id BIGINT NOT NULL REFERENCES members (id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_member_email_token_token
    ON member_email_verification_token (token);

CREATE INDEX IF NOT EXISTS idx_member_email_token_member
    ON member_email_verification_token (member_id);


-- -----------------------------
-- Staff salary monthly ledger
-- -----------------------------
CREATE TABLE IF NOT EXISTS staff_salary_payments (
    id BIGSERIAL PRIMARY KEY,
    staff_id BIGINT NOT NULL REFERENCES staff (id) ON DELETE CASCADE,
    salary_month DATE NOT NULL,
    monthly_salary NUMERIC(10, 2) NOT NULL,
    paid_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    pending_amount NUMERIC(10, 2) NOT NULL DEFAULT 0,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    paid_at TIMESTAMP,
    manually_marked BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT,
    CONSTRAINT uk_staff_salary_staff_month UNIQUE (staff_id, salary_month)
);

CREATE INDEX IF NOT EXISTS idx_staff_salary_staff
    ON staff_salary_payments (staff_id);

CREATE INDEX IF NOT EXISTS idx_staff_salary_month
    ON staff_salary_payments (salary_month);

CREATE INDEX IF NOT EXISTS idx_staff_salary_status
    ON staff_salary_payments (payment_status);
