-- ============================================================
-- V10: Add subject_type column to refresh_tokens.
--
-- Before this migration both User refresh tokens and Member
-- refresh tokens stored their respective IDs in the same
-- user_id column.  Because User IDs and Member IDs are
-- independent auto-increment sequences they will collide (e.g.
-- both can have ID = 5).  revokeAllByUserId(5) would then
-- revoke tokens belonging to both User 5 AND Member 5.
--
-- subject_type = 'USER'   : tokens issued by AuthService /
--                           StaffAuthService
-- subject_type = 'MEMBER' : tokens issued by MemberAuthService
-- ============================================================

ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS subject_type VARCHAR(20) NOT NULL DEFAULT 'USER';

CREATE INDEX IF NOT EXISTS idx_refresh_subject
    ON refresh_tokens (user_id, subject_type);