CREATE TABLE IF NOT EXISTS audit_events (
    id UUID PRIMARY KEY,
    actor_id VARCHAR(255),
    actor_type VARCHAR(32) NOT NULL,
    actor_user_role VARCHAR(255),
    action VARCHAR(255) NOT NULL,
    resource VARCHAR(255) NOT NULL,
    resource_id VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '[]'::jsonb,
    message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_audit_events_actor_id ON audit_events(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_action ON audit_events(action);
CREATE INDEX IF NOT EXISTS idx_audit_events_resource_resource_id ON audit_events(resource, resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_created_at ON audit_events(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_events_metadata_gin ON audit_events USING GIN (metadata);
