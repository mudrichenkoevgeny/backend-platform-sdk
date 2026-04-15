CREATE TABLE IF NOT EXISTS user_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    identifier TEXT NOT NULL,
    identifier_id UUID NOT NULL REFERENCES user_identifiers(id) ON DELETE CASCADE,
    identifier_auth_provider VARCHAR(32) NOT NULL,
    refresh_token_hash TEXT NOT NULL,
    expires_at TIMESTAMPTZ,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    client_type VARCHAR(32),
    user_agent VARCHAR(255),
    ip_address VARCHAR(64),
    language VARCHAR(16),
    device_id TEXT,
    device_name TEXT,
    app_version VARCHAR(64),
    operation_system_version VARCHAR(64),
    last_accessed_at TIMESTAMPTZ,
    last_reauthenticated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_user_sessions_refresh_token_hash ON user_sessions(refresh_token_hash);
CREATE INDEX IF NOT EXISTS idx_user_sessions_identifier ON user_sessions(identifier);
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_device_id ON user_sessions(device_id);