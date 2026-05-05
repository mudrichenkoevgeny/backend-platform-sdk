CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    role VARCHAR(32) NOT NULL,
    account_status VARCHAR(32) NOT NULL,
    account_status_before_deletion VARCHAR(32),
    authority_level INTEGER NOT NULL DEFAULT 0,
    permission_codes JSONB NOT NULL DEFAULT '[]'::jsonb,
    is_totp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMPTZ,
    last_active_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ,
    scheduled_permanent_deletion_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);
CREATE INDEX IF NOT EXISTS idx_users_scheduled_deletion ON users(scheduled_permanent_deletion_at) WHERE scheduled_permanent_deletion_at IS NOT NULL;