-- ============================================================
-- V4: Create subscription_plans and member_subscriptions tables
-- ============================================================

CREATE TABLE IF NOT EXISTS subscription_plans (
                                                  id            BIGSERIAL PRIMARY KEY,
                                                  business_id   BIGINT         NOT NULL REFERENCES businesses (id),
    name          VARCHAR(100)   NOT NULL,
    description   TEXT,
    price         NUMERIC(10, 2) NOT NULL,
    duration_days INT            NOT NULL,
    is_active     BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    version       BIGINT
    );

CREATE INDEX IF NOT EXISTS idx_plan_business ON subscription_plans (business_id);
CREATE INDEX IF NOT EXISTS idx_plan_active   ON subscription_plans (is_active);

-- -------------------------------------------------------
CREATE TABLE IF NOT EXISTS member_subscriptions (
                                                    id          BIGSERIAL PRIMARY KEY,
                                                    member_id   BIGINT         NOT NULL REFERENCES members (id),
    plan_id     BIGINT         NOT NULL REFERENCES subscription_plans (id),
    start_date  DATE           NOT NULL,
    end_date    DATE           NOT NULL,
    status      VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    amount      NUMERIC(10, 2) NOT NULL,
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP      NOT NULL DEFAULT NOW(),
    version     BIGINT
    );

CREATE INDEX IF NOT EXISTS idx_sub_member   ON member_subscriptions (member_id);
CREATE INDEX IF NOT EXISTS idx_sub_status   ON member_subscriptions (status);
CREATE INDEX IF NOT EXISTS idx_sub_end_date ON member_subscriptions (end_date);