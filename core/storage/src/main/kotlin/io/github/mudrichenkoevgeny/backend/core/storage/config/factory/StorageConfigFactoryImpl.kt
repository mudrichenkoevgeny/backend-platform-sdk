package io.github.mudrichenkoevgeny.backend.core.storage.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.storage.config.envkeys.StorageEnvKeys
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.ImgproxyConfig
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.ImgproxyPreset
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.ImgproxyResizeType
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

        val isImgproxyEnabled = envReader.getByKeyOrNull(StorageEnvKeys.STORAGE_IMGPROXY_ENABLED)?.toBooleanStrictOrNull() ?: false
        val imgproxyBaseUrl = envReader.getByKeyOrNull(StorageEnvKeys.STORAGE_IMGPROXY_BASE_URL).orEmpty()

        val imgproxyKeyFile = envReader.getByKeyOrNull(StorageEnvKeys.STORAGE_IMGPROXY_KEY_FILE)
        val imgproxyKey = if (!imgproxyKeyFile.isNullOrBlank()) {
            envReader.readSecret(imgproxyKeyFile)
        } else {
            envReader.getByKeyOrNull(StorageEnvKeys.STORAGE_IMGPROXY_KEY).orEmpty()
        }

        val imgproxySaltFile = envReader.getByKeyOrNull(StorageEnvKeys.STORAGE_IMGPROXY_SALT_FILE)
        val imgproxySalt = if (!imgproxySaltFile.isNullOrBlank()) {
            envReader.readSecret(imgproxySaltFile)
        } else {
            envReader.getByKeyOrNull(StorageEnvKeys.STORAGE_IMGPROXY_SALT).orEmpty()
        }
        val imgproxyPresetsString = envReader.getByKeyOrNull(StorageEnvKeys.STORAGE_IMGPROXY_PRESETS)
        val imgproxyVideoThumbnailSecond = envReader.getByKeyOrNull(StorageEnvKeys.STORAGE_IMGPROXY_VIDEO_THUMBNAIL_SECOND)?.toIntOrNull() ?: 2

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

        val imgproxyConfig = if (isImgproxyEnabled) {
            ImgproxyConfig(
                isEnabled = true,
                baseUrl = imgproxyBaseUrl,
                key = imgproxyKey,
                salt = imgproxySalt,
                presets = parsePresets(imgproxyPresetsString),
                videoThumbnailSecond = imgproxyVideoThumbnailSecond
            )
        } else {
            null
        }

        return StorageConfig(
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
    }

    private fun parsePresets(presetsString: String?): Map<String, ImgproxyPreset> {
        if (presetsString.isNullOrBlank()) {
            return emptyMap()
        }

        val presets = mutableMapOf<String, ImgproxyPreset>()
        val entries = presetsString.split(",")
        for (entry in entries) {
            val parts = entry.trim().split("=")
            if (parts.size != 2) continue
            val presetKey = parts[0].trim()
            val specParts = parts[1].trim().split(":")
            if (specParts.size != 2) continue
            val size = specParts[0].toIntOrNull() ?: continue
            val resizeType = ImgproxyResizeType.fromValueOrNull(specParts[1]) ?: continue
            if (presetKey.isNotBlank() && size > 0) {
                presets[presetKey] = ImgproxyPreset(sizePixels = size, resizeType = resizeType)
            }
        }
        return presets
    }
}
