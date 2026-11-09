package io.github.mudrichenkoevgeny.backend.core.storage.config.model

import io.github.mudrichenkoevgeny.backend.core.storage.service.StorageService
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.storagesettings.ManagementStorageSettings

/**
 * Configuration for the storage module.
 *
 * Provides S3/object storage connection and policy settings.
 *
 * @param s3Endpoint S3 endpoint URL (can be AWS or S3-compatible services such as MinIO).
 * @param s3Region AWS region name used by the S3 client.
 * @param s3AccessKey S3 access key (usually read from a secret file).
 * @param s3SecretKey S3 secret key (usually read from a secret file).
 * @param s3BucketNamePrivate default S3 bucket name used for storing private files.
 * @param s3BucketNamePublic default S3 bucket name used for storing public files.
 * @param s3PublicUrl base public URL used to build object URLs returned by [StorageService.getUrl].
 * @param forcePathStyle whether to force path-style S3 addressing (useful for some S3-compatible providers).
 * @param maintenanceBatchSize maximum number of file records to process in a single pass during background scheduled jobs.
 * @param managementStorageSettings default platform-wide storage settings parsed from configuration/env.
 * @param imgproxyConfig optional imgproxy service configuration for generating media previews and thumbnails.
 */
public data class StorageConfig(
    val s3Endpoint: String,
    val s3Region: String,
    val s3AccessKey: String,
    val s3SecretKey: String,
    val s3BucketNamePrivate: String,
    val s3BucketNamePublic: String,
    val s3PublicUrl: String,
    val forcePathStyle: Boolean,
    val maintenanceBatchSize: Int,
    val managementStorageSettings: ManagementStorageSettings,
    val imgproxyConfig: ImgproxyConfig? = null
)
