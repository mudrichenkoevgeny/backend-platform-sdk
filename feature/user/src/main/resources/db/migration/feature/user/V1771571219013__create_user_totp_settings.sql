CREATE TABLE IF NOT EXISTS user_totp_settings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    encrypted_secret VARCHAR(255) NOT NULL,
    is_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    encrypted_recovery_codes JSONB,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_totp_settings_user_id ON user_totp_settings(user_id);
CREATE INDEX IF NOT EXISTS idx_user_totp_settings_created_at ON user_totp_settings(created_at);