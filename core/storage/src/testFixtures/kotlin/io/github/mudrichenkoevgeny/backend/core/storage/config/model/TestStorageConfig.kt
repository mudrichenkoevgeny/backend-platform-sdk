package io.github.mudrichenkoevgeny.backend.core.storage.config.model

import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.storagesettings.ManagementStorageSettings

fun createTestStorageConfig(
    s3Endpoint: String = "",
    s3Region: String = "",
    s3AccessKey: String = "",
    s3SecretKey: String = "",
    s3BucketName: String = "",
    s3PublicUrl: String = "https://example.com/files",
    forcePathStyle: Boolean = false,
    maintenanceBatchSize: Int = 100,
    managementStorageSettings: ManagementStorageSettings = ManagementStorageSettings(
        minUploadSizeBytes = 1,
        maxUploadSizeBytes = 52428800,
        totalStorageQuotaForOpenUserBytes = 1073741824,
        totalStorageQuotaForManagementUserBytes = null,
        presignedUrlExpirationSeconds = 900,
        maxPendingUploadsForOpenUser = 10,
        maxPendingUploadsForManagementUser = 100,
        isDeduplicationEnabled = true,
        deletedFilesCleanupIntervalSeconds = 86400
    )
) = StorageConfig(
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
