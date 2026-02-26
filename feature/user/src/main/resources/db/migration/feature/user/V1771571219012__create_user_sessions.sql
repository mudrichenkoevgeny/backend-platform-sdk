CREATE TABLE IF NOT EXISTS user_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_identifier_id UUID NOT NULL REFERENCES user_identifiers(id) ON DELETE CASCADE,
    user_identifier_auth_provider VARCHAR(32) NOT NULL,
    token_hash TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    user_client_type VARCHAR(32),
    user_agent VARCHAR(255),
    ip_address VARCHAR(64),
    language VARCHAR(16),
    device_id TEXT,
    device_name TEXT,
    app_version VARCHAR(64),
    operation_system_version VARCHAR(64),
    last_accessed_at TIMESTAMPTZ NOT NULL,
    last_reauthenticated_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_user_sessions_token_hash ON user_sessions(token_hash);
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_device_id ON user_sessions(device_id);