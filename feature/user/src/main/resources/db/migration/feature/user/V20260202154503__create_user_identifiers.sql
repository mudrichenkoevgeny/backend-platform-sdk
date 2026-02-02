CREATE TABLE IF NOT EXISTS user_identifiers (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_auth_provider VARCHAR(32) NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_identifiers_userid_provider
    ON user_identifiers(user_id, user_auth_provider);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_identifiers_provider_identifier
    ON user_identifiers(user_auth_provider, identifier);