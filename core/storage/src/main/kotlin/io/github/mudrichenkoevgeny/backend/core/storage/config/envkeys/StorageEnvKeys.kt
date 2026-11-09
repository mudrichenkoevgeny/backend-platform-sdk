package io.github.mudrichenkoevgeny.backend.core.storage.config.envkeys

import io.github.mudrichenkoevgeny.backend.core.storage.config.factory.StorageConfigFactory
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig

/**
 * Environment variable names used to build [StorageConfig].
 *
 * Consumed by [StorageConfigFactory] implementations.
 */
public object StorageEnvKeys {
    /** S3 endpoint URL. */
    public const val S3_ENDPOINT: String = "S3_ENDPOINT"

    /** S3 region name. */
    public const val S3_REGION: String = "S3_REGION"

    /** Default S3 bucket name for private files. */
    public const val S3_BUCKET_NAME_PRIVATE: String = "S3_BUCKET_NAME_PRIVATE"

    /** Default S3 bucket name for public files. */
    public const val S3_BUCKET_NAME_PUBLIC: String = "S3_BUCKET_NAME_PUBLIC"

    /** Base public URL used to build object URLs. */
    public const val S3_PUBLIC_URL: String = "S3_PUBLIC_URL"

    /** Whether to force path-style S3 addressing ("true"/"false"). */
    public const val S3_FORCE_PATH_STYLE: String = "S3_FORCE_PATH_STYLE"

    /** Secret file path for the S3 access key. */
    public const val S3_ACCESS_KEY_FILE: String = "S3_ACCESS_KEY_FILE"

    /** Secret file path for the S3 secret key. */
    public const val S3_SECRET_KEY_FILE: String = "S3_SECRET_KEY_FILE"

    /** Minimum permitted single file upload size in bytes. */
    public const val STORAGE_MIN_UPLOAD_SIZE_BYTES: String = "STORAGE_MIN_UPLOAD_SIZE_BYTES"

    /** Maximum permitted single file upload size in bytes. */
    public const val STORAGE_MAX_UPLOAD_SIZE_BYTES: String = "STORAGE_MAX_UPLOAD_SIZE_BYTES"

    /** Cumulative storage quota in bytes per regular open user. */
    public const val STORAGE_TOTAL_QUOTA_OPEN_USER_BYTES: String = "STORAGE_TOTAL_QUOTA_OPEN_USER_BYTES"

    /** Cumulative storage quota in bytes per management user. */
    public const val STORAGE_TOTAL_QUOTA_MANAGEMENT_USER_BYTES: String =
        "STORAGE_TOTAL_QUOTA_MANAGEMENT_USER_BYTES"

    /** Presigned URL expiration duration in seconds. */
    public const val STORAGE_PRESIGNED_URL_EXPIRATION_SECONDS: String = "STORAGE_PRESIGNED_URL_EXPIRATION_SECONDS"

    /** Maximum concurrent unfinalized upload sessions for open users. */
    public const val STORAGE_MAX_PENDING_UPLOADS_OPEN_USER: String = "STORAGE_MAX_PENDING_UPLOADS_OPEN_USER"

    /** Maximum concurrent unfinalized upload sessions for management users. */
    public const val STORAGE_MAX_PENDING_UPLOADS_MANAGEMENT_USER: String = "STORAGE_MAX_PENDING_UPLOADS_MANAGEMENT_USER"

    /** Whether Content-Addressable Storage (CAS) file deduplication is enabled. */
    public const val STORAGE_IS_DEDUPLICATION_ENABLED: String = "STORAGE_IS_DEDUPLICATION_ENABLED"

    /** Interval in seconds for physical cleanup of soft-deleted files from S3/storage. */
    public const val STORAGE_DELETED_FILES_CLEANUP_INTERVAL_SECONDS: String =
        "STORAGE_DELETED_FILES_CLEANUP_INTERVAL_SECONDS"

    /** Batch size for background maintenance jobs. */
    public const val STORAGE_MAINTENANCE_BATCH_SIZE: String = "STORAGE_MAINTENANCE_BATCH_SIZE"

    /** Whether imgproxy image/video preview and thumbnail generation is enabled. */
    public const val STORAGE_IMGPROXY_ENABLED: String = "STORAGE_IMGPROXY_ENABLED"

    /** Base public URL of the imgproxy service. */
    public const val STORAGE_IMGPROXY_BASE_URL: String = "STORAGE_IMGPROXY_BASE_URL"

    /** Secret file path for the imgproxy HMAC-SHA256 secret key. */
    public const val STORAGE_IMGPROXY_KEY_FILE: String = "STORAGE_IMGPROXY_KEY_FILE"

    /** Secret file path for the imgproxy HMAC-SHA256 salt. */
    public const val STORAGE_IMGPROXY_SALT_FILE: String = "STORAGE_IMGPROXY_SALT_FILE"

    /** Hex-encoded secret key used for imgproxy HMAC-SHA256 URL signing (fallback if key file is not provided). */
    public const val STORAGE_IMGPROXY_KEY: String = "STORAGE_IMGPROXY_KEY"

    /** Hex-encoded salt used for imgproxy HMAC-SHA256 URL signing (fallback if salt file is not provided). */
    public const val STORAGE_IMGPROXY_SALT: String = "STORAGE_IMGPROXY_SALT"

    /** String of preset specifications for imgproxy (format: "thumbnail=150:fill,preview=600:fit"). */
    public const val STORAGE_IMGPROXY_PRESETS: String = "STORAGE_IMGPROXY_PRESETS"

    /** Default video seek timestamp in seconds when extracting video thumbnail frames. */
    public const val STORAGE_IMGPROXY_VIDEO_THUMBNAIL_SECOND: String = "STORAGE_IMGPROXY_VIDEO_THUMBNAIL_SECOND"
}
