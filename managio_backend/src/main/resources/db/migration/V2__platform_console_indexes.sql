-- The platform console (D-028) counts activity and sessions across all businesses by time.
-- The existing indexes lead with business_id or subject_id, which those queries can't use.
CREATE INDEX ix_activity_created ON activity_events (created_at);
CREATE INDEX ix_refresh_tokens_type_created ON refresh_tokens (subject_type, created_at);
