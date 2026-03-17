package io.github.mudrichenkoevgeny.backend.core.storage.config.model

import io.github.mudrichenkoevgeny.backend.core.storage.model.StorageType
import io.github.mudrichenkoevgeny.backend.core.storage.service.StorageService

/**
 * Configuration for the storage module.
 *
 * Provides both S3 and local filesystem settings; the active backend is selected by [storageType].
 *
 * @param storageType selected storage backend type.
 * @param s3Endpoint S3 endpoint URL (can be AWS or S3-compatible services such as MinIO).
 * @param s3Region AWS region name used by the S3 client.
 * @param s3AccessKey S3 access key (usually read from a secret file).
 * @param s3SecretKey S3 secret key (usually read from a secret file).
 * @param s3BucketName default S3 bucket name used when no bucket is explicitly provided to [StorageService].
 * @param s3PublicUrl base public URL used to build object URLs returned by [StorageService.getUrl].
 * @param forcePathStyle whether to force path-style S3 addressing (useful for some S3-compatible providers).
 * @param localStoragePath filesystem path used as a root directory for local storage.
 */
data class StorageConfig(
    val storageType: StorageType,
    val s3Endpoint: String,
    val s3Region: String,
    val s3AccessKey: String,
    val s3SecretKey: String,
    val s3BucketName: String,
    val s3PublicUrl: String,
    val forcePathStyle: Boolean,
    val localStoragePath: String
)