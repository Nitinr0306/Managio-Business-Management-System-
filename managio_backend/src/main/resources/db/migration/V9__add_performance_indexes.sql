-- ============================================================
-- V9: Additional indexes for frequently executed queries.
-- ============================================================

-- Subscription plan active lookups (used by every plan list request)
CREATE INDEX IF NOT EXISTS idx_plan_business_active
    ON subscription_plans (business_id, is_active);

-- Active subscription lookups by member (hot path in dashboards)
CREATE INDEX IF NOT EXISTS idx_sub_member_status
    ON member_subscriptions (member_id, status);

-- Expiry queries (used by scheduler + dashboards every request)
CREATE INDEX IF NOT EXISTS idx_sub_end_status
    ON member_subscriptions (end_date, status)
    WHERE status = 'ACTIVE';

-- Payment aggregation by member (used in revenue calculations)
CREATE INDEX IF NOT EXISTS idx_payment_member_created
    ON payments (member_id, created_at DESC);

-- Staff lookup by userId across businesses (login hot path)
CREATE INDEX IF NOT EXISTS idx_staff_user_active
    ON staff (user_id, status)
    WHERE deleted_at IS NULL;

-- Auth audit log cleanup queries
CREATE INDEX IF NOT EXISTS idx_auth_audit_email_event
    ON auth_audit_logs (email, event_type, created_at DESC);