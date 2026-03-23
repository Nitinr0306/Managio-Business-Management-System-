-- Staff tasks module + audit entity public ID enrichment

ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS entity_public_id VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_audit_entity_public_id
    ON audit_logs (entity_public_id);

CREATE TABLE IF NOT EXISTS staff_tasks (
    id BIGSERIAL PRIMARY KEY,
    public_id VARCHAR(20) NOT NULL UNIQUE,
    business_id BIGINT NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    due_date DATE,
    assigned_staff_id BIGINT REFERENCES staff (id) ON DELETE SET NULL,
    created_by_user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT
);

CREATE INDEX IF NOT EXISTS idx_staff_tasks_business
    ON staff_tasks (business_id);

CREATE INDEX IF NOT EXISTS idx_staff_tasks_status
    ON staff_tasks (status);

CREATE INDEX IF NOT EXISTS idx_staff_tasks_priority
    ON staff_tasks (priority);

CREATE INDEX IF NOT EXISTS idx_staff_tasks_assignee
    ON staff_tasks (assigned_staff_id);

CREATE INDEX IF NOT EXISTS idx_staff_tasks_due
    ON staff_tasks (due_date);
