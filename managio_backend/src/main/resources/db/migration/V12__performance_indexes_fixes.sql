-- ============================================================
-- V12: Performance indexes for new queries added in fixes
-- ============================================================

-- MemberRepository.countNewMembersSince()
-- Used by BusinessStatisticsService and OwnerDashboardService for
-- "new members this month" metric. Without this index the query
-- does a full scan of the members table filtered by businessId + createdAt.
CREATE INDEX IF NOT EXISTS idx_member_business_created
    ON members (business_id, created_at DESC)
    WHERE deleted_at IS NULL;

-- MemberSubscriptionRepository.countExpiredByBusinessId()
-- Used by BusinessStatisticsService for expired subscription count.
-- Partial index on ACTIVE+EXPIRED to keep it small.
CREATE INDEX IF NOT EXISTS idx_sub_member_end_status
    ON member_subscriptions (member_id, end_date, status);

-- Composite covering index for staff login hot path:
-- findByBusinessIdAndUserId already has idx_staff_business and idx_staff_user,
-- but a composite makes the tenant-user lookup a single index seek.
CREATE INDEX IF NOT EXISTS idx_staff_business_user_active
    ON staff (business_id, user_id)
    WHERE deleted_at IS NULL;