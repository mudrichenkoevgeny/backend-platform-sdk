CREATE TABLE IF NOT EXISTS storage_files (
    id UUID PRIMARY KEY,
    owner_user_id VARCHAR(255),
    file_name VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(255) NOT NULL,
    category VARCHAR(32) NOT NULL,
    visibility VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    target_scope VARCHAR(255),
    content_hash VARCHAR(255),
    s3_bucket VARCHAR(255) NOT NULL,
    s3_key VARCHAR(1024) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_storage_files_owner_status_created
ON storage_files(owner_user_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_storage_files_content_hash
ON storage_files(content_hash) WHERE content_hash IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_storage_files_created_at
ON storage_files(created_at DESC);