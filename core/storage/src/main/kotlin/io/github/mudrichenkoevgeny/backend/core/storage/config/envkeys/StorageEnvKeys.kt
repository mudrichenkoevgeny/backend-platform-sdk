package io.github.mudrichenkoevgeny.backend.core.storage.config.envkeys

import io.github.mudrichenkoevgeny.backend.core.storage.config.factory.StorageConfigFactory
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig

/**
 * Environment variable names used to build [StorageConfig].
 *
 * Consumed by [StorageConfigFactory] implementations.
 */
object StorageEnvKeys {
    /** S3 endpoint URL. */
    const val S3_ENDPOINT = "S3_ENDPOINT"

    /** S3 region name. */
    const val S3_REGION = "S3_REGION"

    /** Default S3 bucket name. */
    const val S3_BUCKET_NAME = "S3_BUCKET_NAME"

    /** Base public URL used to build object URLs. */
    const val S3_PUBLIC_URL = "S3_PUBLIC_URL"

    /** Whether to force path-style S3 addressing ("true"/"false"). */
    const val S3_FORCE_PATH_STYLE = "S3_FORCE_PATH_STYLE"

    /** Secret file path for the S3 access key. */
    const val S3_ACCESS_KEY_FILE = "S3_ACCESS_KEY_FILE"

    /** Secret file path for the S3 secret key. */
    const val S3_SECRET_KEY_FILE = "S3_SECRET_KEY_FILE"

    /** Minimum permitted single file upload size in bytes. */
    const val STORAGE_MIN_UPLOAD_SIZE_BYTES = "STORAGE_MIN_UPLOAD_SIZE_BYTES"

    /** Maximum permitted single file upload size in bytes. */
    const val STORAGE_MAX_UPLOAD_SIZE_BYTES = "STORAGE_MAX_UPLOAD_SIZE_BYTES"

    /** Cumulative storage quota in bytes per regular open user. */
    const val STORAGE_TOTAL_QUOTA_OPEN_USER_BYTES = "STORAGE_TOTAL_QUOTA_OPEN_USER_BYTES"

    /** Cumulative storage quota in bytes per management user. */
    const val STORAGE_TOTAL_QUOTA_MANAGEMENT_USER_BYTES = "STORAGE_TOTAL_QUOTA_MANAGEMENT_USER_BYTES"

    /** Presigned URL expiration duration in seconds. */
    const val STORAGE_PRESIGNED_URL_EXPIRATION_SECONDS = "STORAGE_PRESIGNED_URL_EXPIRATION_SECONDS"

    /** Maximum concurrent unfinalized upload sessions for open users. */
    const val STORAGE_MAX_PENDING_UPLOADS_OPEN_USER = "STORAGE_MAX_PENDING_UPLOADS_OPEN_USER"

    /** Maximum concurrent unfinalized upload sessions for management users. */
    const val STORAGE_MAX_PENDING_UPLOADS_MANAGEMENT_USER = "STORAGE_MAX_PENDING_UPLOADS_MANAGEMENT_USER"

    /** Whether Content-Addressable Storage (CAS) file deduplication is enabled. */
    const val STORAGE_IS_DEDUPLICATION_ENABLED = "STORAGE_IS_DEDUPLICATION_ENABLED"

    /** Interval in seconds for physical cleanup of soft-deleted files from S3/storage. */
    const val STORAGE_DELETED_FILES_CLEANUP_INTERVAL_SECONDS = "STORAGE_DELETED_FILES_CLEANUP_INTERVAL_SECONDS"

    /** Batch size for background maintenance jobs. */
    const val STORAGE_MAINTENANCE_BATCH_SIZE = "STORAGE_MAINTENANCE_BATCH_SIZE"

    /** Whether imgproxy image/video preview and thumbnail generation is enabled. */
    const val STORAGE_IMGPROXY_ENABLED = "STORAGE_IMGPROXY_ENABLED"

    /** Base public URL of the imgproxy service. */
    const val STORAGE_IMGPROXY_BASE_URL = "STORAGE_IMGPROXY_BASE_URL"

    /** Secret file path for the imgproxy HMAC-SHA256 secret key. */
    const val STORAGE_IMGPROXY_KEY_FILE = "STORAGE_IMGPROXY_KEY_FILE"

    /** Secret file path for the imgproxy HMAC-SHA256 salt. */
    const val STORAGE_IMGPROXY_SALT_FILE = "STORAGE_IMGPROXY_SALT_FILE"

    /** Hex-encoded secret key used for imgproxy HMAC-SHA256 URL signing (fallback if key file is not provided). */
    const val STORAGE_IMGPROXY_KEY = "STORAGE_IMGPROXY_KEY"

    /** Hex-encoded salt used for imgproxy HMAC-SHA256 URL signing (fallback if salt file is not provided). */
    const val STORAGE_IMGPROXY_SALT = "STORAGE_IMGPROXY_SALT"

    /** String of preset specifications for imgproxy (format: "thumbnail=150:fill,preview=600:fit"). */
    const val STORAGE_IMGPROXY_PRESETS = "STORAGE_IMGPROXY_PRESETS"

    /** Default video seek timestamp in seconds when extracting video thumbnail frames. */
    const val STORAGE_IMGPROXY_VIDEO_THUMBNAIL_SECOND = "STORAGE_IMGPROXY_VIDEO_THUMBNAIL_SECOND"
}
