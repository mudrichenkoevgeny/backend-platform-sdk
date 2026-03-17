package io.github.mudrichenkoevgeny.backend.core.storage.config.envkeys

import io.github.mudrichenkoevgeny.backend.core.storage.config.factory.StorageConfigFactory
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig

/**
 * Environment variable names used to build [StorageConfig].
 *
 * Consumed by [StorageConfigFactory] implementations.
 */
object StorageEnvKeys {
    /** Storage backend type: "S3" or "LOCAL". */
    const val STORAGE_TYPE = "STORAGE_TYPE"
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
    /** Root directory for local filesystem storage. */
    const val LOCAL_STORAGE_PATH = "LOCAL_STORAGE_PATH"
    /** Secret file path for the S3 access key. */
    const val S3_ACCESS_KEY_FILE = "S3_ACCESS_KEY_FILE"
    /** Secret file path for the S3 secret key. */
    const val S3_SECRET_KEY_FILE = "S3_SECRET_KEY_FILE"
}