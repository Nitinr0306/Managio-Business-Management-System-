-- ============================================================
-- V8: Add extended fields to businesses table.
--     These were present in the Business entity (FIX 11) but
--     were never added to the initial V2 migration, causing
--     Hibernate schema-validate to fail on startup.
-- ============================================================

ALTER TABLE businesses
    ADD COLUMN IF NOT EXISTS type        VARCHAR(50),
    ADD COLUMN IF NOT EXISTS description VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS city        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS state       VARCHAR(100),
    ADD COLUMN IF NOT EXISTS country     VARCHAR(100);

-- Useful composite index for city/state/country lookups
CREATE INDEX IF NOT EXISTS idx_business_location
    ON businesses (city, state, country)
    WHERE deleted_at IS NULL;