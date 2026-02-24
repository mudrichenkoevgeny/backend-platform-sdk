CREATE TABLE IF NOT EXISTS system_settings (
    id UUID PRIMARY KEY,
    key VARCHAR(255) NOT NULL UNIQUE,
    value TEXT NOT NULL,
    type VARCHAR(32) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_server_settings_key ON server_settings(key);
CREATE INDEX IF NOT EXISTS idx_server_settings_created_at ON server_settings(created_at);