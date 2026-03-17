-- ============================================================
-- V5: Create payments table
-- ============================================================

CREATE TABLE IF NOT EXISTS payments (
                                        id             BIGSERIAL PRIMARY KEY,
                                        member_id      BIGINT         NOT NULL REFERENCES members (id),
    subscription_id BIGINT        REFERENCES member_subscriptions (id),
    amount         NUMERIC(10, 2) NOT NULL,
    payment_method VARCHAR(20)    NOT NULL,
    notes          VARCHAR(500),
    recorded_by    BIGINT         NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_payment_member       ON payments (member_id);
CREATE INDEX IF NOT EXISTS idx_payment_subscription ON payments (subscription_id);
CREATE INDEX IF NOT EXISTS idx_payment_created      ON payments (created_at);
CREATE INDEX IF NOT EXISTS idx_payment_method       ON payments (payment_method);