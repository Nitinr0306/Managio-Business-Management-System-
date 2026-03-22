-- ============================================================
-- V13: Add reference_number and paid_at columns to payments
-- ============================================================

ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS reference_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS paid_at          TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_payment_paid_at ON payments (paid_at);
