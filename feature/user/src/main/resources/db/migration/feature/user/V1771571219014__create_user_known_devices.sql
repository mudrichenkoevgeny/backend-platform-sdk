CREATE TABLE IF NOT EXISTS user_known_devices (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id TEXT NOT NULL,
    first_ip_address VARCHAR(64),
    last_ip_address VARCHAR(64),
    user_agent VARCHAR(255),
    first_seen_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_known_devices_user_device ON user_known_devices(user_id, device_id);
