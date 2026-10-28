package io.github.mudrichenkoevgeny.backend.core.storage.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.storage.config.envkeys.StorageEnvKeys
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.storagesettings.ManagementStorageSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [StorageConfigFactory] implementation that builds [StorageConfig] from environment variables and secret files.
 *
 * Reads S3 access/secret keys from files whose paths are provided via [StorageEnvKeys.S3_ACCESS_KEY_FILE] and
 * [StorageEnvKeys.S3_SECRET_KEY_FILE], using [EnvReader].
 */
@Singleton
class StorageConfigFactoryImpl @Inject constructor(
    private val envReader: EnvReader
) : StorageConfigFactory {

    override fun create(): StorageConfig {
        val s3AccessKeyFile = envReader.getByKey(StorageEnvKeys.S3_ACCESS_KEY_FILE)
        val s3SecretKeyFile = envReader.getByKey(StorageEnvKeys.S3_SECRET_KEY_FILE)

        val s3Endpoint = envReader.getByKey(StorageEnvKeys.S3_ENDPOINT)
        val s3Region = envReader.getByKey(StorageEnvKeys.S3_REGION)
        val s3BucketName = envReader.getByKey(StorageEnvKeys.S3_BUCKET_NAME)
        val s3PublicUrl = envReader.getByKey(StorageEnvKeys.S3_PUBLIC_URL)
        val forcePathStyle = envReader.getByKey(StorageEnvKeys.S3_FORCE_PATH_STYLE).toBoolean()

        val minUploadSizeBytes = envReader.getByKey(StorageEnvKeys.STORAGE_MIN_UPLOAD_SIZE_BYTES).toLong()
        val maxUploadSizeBytes = envReader.getByKey(StorageEnvKeys.STORAGE_MAX_UPLOAD_SIZE_BYTES).toLong()
        val totalStorageQuotaForOpenUserBytes = envReader.getByKey(StorageEnvKeys.STORAGE_TOTAL_QUOTA_OPEN_USER_BYTES).toLongOrNull()
        val totalStorageQuotaForManagementUserBytes = envReader.getByKey(StorageEnvKeys.STORAGE_TOTAL_QUOTA_MANAGEMENT_USER_BYTES).toLongOrNull()
        val presignedUrlExpirationSeconds = envReader.getByKey(StorageEnvKeys.STORAGE_PRESIGNED_URL_EXPIRATION_SECONDS).toInt()
        val maxPendingUploadsForOpenUser = envReader.getByKey(StorageEnvKeys.STORAGE_MAX_PENDING_UPLOADS_OPEN_USER).toInt()
        val maxPendingUploadsForManagementUser = envReader.getByKey(StorageEnvKeys.STORAGE_MAX_PENDING_UPLOADS_MANAGEMENT_USER).toInt()
        val isDeduplicationEnabled = envReader.getByKeyOrNull(StorageEnvKeys.STORAGE_IS_DEDUPLICATION_ENABLED)?.toBooleanStrictOrNull() ?: true
        val deletedFilesCleanupIntervalSeconds = envReader.getByKeyOrNull(StorageEnvKeys.STORAGE_DELETED_FILES_CLEANUP_INTERVAL_SECONDS)?.toIntOrNull() ?: 86400
        val maintenanceBatchSize = envReader.getByKeyOrNull(StorageEnvKeys.STORAGE_MAINTENANCE_BATCH_SIZE)?.toIntOrNull() ?: 100

        val s3AccessKey = envReader.readSecret(s3AccessKeyFile)
        val s3SecretKey = envReader.readSecret(s3SecretKeyFile)

        val managementStorageSettings = ManagementStorageSettings(
            minUploadSizeBytes = minUploadSizeBytes,
            maxUploadSizeBytes = maxUploadSizeBytes,
            totalStorageQuotaForOpenUserBytes = totalStorageQuotaForOpenUserBytes,
            totalStorageQuotaForManagementUserBytes = totalStorageQuotaForManagementUserBytes,
            presignedUrlExpirationSeconds = presignedUrlExpirationSeconds,
            maxPendingUploadsForOpenUser = maxPendingUploadsForOpenUser,
            maxPendingUploadsForManagementUser = maxPendingUploadsForManagementUser,
            isDeduplicationEnabled = isDeduplicationEnabled,
            deletedFilesCleanupIntervalSeconds = deletedFilesCleanupIntervalSeconds
        )

        return StorageConfig(
            s3Endpoint = s3Endpoint,
            s3Region = s3Region,
            s3AccessKey = s3AccessKey,
            s3SecretKey = s3SecretKey,
            s3BucketName = s3BucketName,
            s3PublicUrl = s3PublicUrl,
            forcePathStyle = forcePathStyle,
            maintenanceBatchSize = maintenanceBatchSize,
            managementStorageSettings = managementStorageSettings
        )
    }
}
