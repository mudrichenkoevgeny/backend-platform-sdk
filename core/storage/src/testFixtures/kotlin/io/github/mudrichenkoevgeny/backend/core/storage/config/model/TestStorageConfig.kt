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
    ),
    imgproxyConfig: ImgproxyConfig? = null
) = StorageConfig(
    s3Endpoint = s3Endpoint,
    s3Region = s3Region,
    s3AccessKey = s3AccessKey,
    s3SecretKey = s3SecretKey,
    s3BucketName = s3BucketName,
    s3PublicUrl = s3PublicUrl,
    forcePathStyle = forcePathStyle,
    maintenanceBatchSize = maintenanceBatchSize,
    managementStorageSettings = managementStorageSettings,
    imgproxyConfig = imgproxyConfig
)

fun createTestImgproxyConfig(
    isEnabled: Boolean = true,
    baseUrl: String = "http://localhost:8081",
    key: String = "943f4c9b9fd6938a00ef01a6cbd990552a8f220d51832e0c5294f8cb66d6f380",
    salt: String = "4c0e123a968603f48c8b6727d9284242",
    presets: Map<String, ImgproxyPreset> = mapOf(
        "thumbnail" to ImgproxyPreset(150, ImgproxyResizeType.FILL),
        "preview" to ImgproxyPreset(600, ImgproxyResizeType.FIT)
    ),
    videoThumbnailSecond: Int = 2
) = ImgproxyConfig(
    isEnabled = isEnabled,
    baseUrl = baseUrl,
    key = key,
    salt = salt,
    presets = presets,
    videoThumbnailSecond = videoThumbnailSecond
)
