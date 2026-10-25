package io.github.mudrichenkoevgeny.backend.core.storage.settings.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapSuccess
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.storagesettings.ManagementStorageSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.storagesettings.OpenStorageSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [StorageSettingsProvider] implementation backed by [SystemSettingsService].
 *
 * Persists and caches system settings in PostgreSQL and Redis.
 *
 * @property settingsService System settings service for storage and synchronization.
 * @property config Storage module configuration holding default values.
 */
@Singleton
class StorageSettingsProviderImpl @Inject constructor(
    private val settingsService: SystemSettingsService,
    private val config: StorageConfig
) : StorageSettingsProvider {

    override suspend fun initialize(): AppResult<Unit> {
        val defaults = config.managementStorageSettings
        val defaultSettings = listOf(
            SystemSetting(
                key = KEY_MIN_UPLOAD_SIZE_BYTES,
                value = "${defaults.minUploadSizeBytes}",
                type = SettingType.LONG
            ),
            SystemSetting(
                key = KEY_MAX_UPLOAD_SIZE_BYTES,
                value = "${defaults.maxUploadSizeBytes}",
                type = SettingType.LONG
            ),
            SystemSetting(
                key = KEY_TOTAL_STORAGE_QUOTA_FOR_OPEN_USER_BYTES,
                value = defaults.totalStorageQuotaForOpenUserBytes?.toString() ?: "",
                type = SettingType.STRING
            ),
            SystemSetting(
                key = KEY_TOTAL_STORAGE_QUOTA_FOR_MANAGEMENT_USER_BYTES,
                value = defaults.totalStorageQuotaForManagementUserBytes?.toString() ?: "",
                type = SettingType.STRING
            ),
            SystemSetting(
                key = KEY_PRESIGNED_URL_EXPIRATION_SECONDS,
                value = "${defaults.presignedUrlExpirationSeconds}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_MAX_PENDING_UPLOADS_FOR_OPEN_USER,
                value = "${defaults.maxPendingUploadsForOpenUser}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_MAX_PENDING_UPLOADS_FOR_MANAGEMENT_USER,
                value = "${defaults.maxPendingUploadsForManagementUser}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_IS_DEDUPLICATION_ENABLED,
                value = "${defaults.isDeduplicationEnabled}",
                type = SettingType.BOOLEAN
            ),
            SystemSetting(
                key = KEY_DELETED_FILES_CLEANUP_INTERVAL_SECONDS,
                value = "${defaults.deletedFilesCleanupIntervalSeconds}",
                type = SettingType.INT
            )
        )
        return settingsService.registerDefaults(defaultSettings)
    }

    override fun getManagementStorageSettings(): ManagementStorageSettings {
        return ManagementStorageSettings(
            minUploadSizeBytes = getMinUploadSizeBytes(),
            maxUploadSizeBytes = getMaxUploadSizeBytes(),
            totalStorageQuotaForOpenUserBytes = getTotalStorageQuotaForOpenUserBytes(),
            totalStorageQuotaForManagementUserBytes = getTotalStorageQuotaForManagementUserBytes(),
            presignedUrlExpirationSeconds = getPresignedUrlExpirationSeconds(),
            maxPendingUploadsForOpenUser = getMaxPendingUploadsForOpenUser(),
            maxPendingUploadsForManagementUser = getMaxPendingUploadsForManagementUser(),
            isDeduplicationEnabled = getIsDeduplicationEnabled(),
            deletedFilesCleanupIntervalSeconds = getDeletedFilesCleanupIntervalSeconds()
        )
    }

    override fun getOpenStorageSettings(): OpenStorageSettings {
        return OpenStorageSettings(
            minUploadSizeBytes = getMinUploadSizeBytes(),
            maxUploadSizeBytes = getMaxUploadSizeBytes()
        )
    }

    override fun getMinUploadSizeBytes(): Long {
        return settingsService.getLong(KEY_MIN_UPLOAD_SIZE_BYTES)
            ?: config.managementStorageSettings.minUploadSizeBytes
    }

    override fun getMaxUploadSizeBytes(): Long {
        return settingsService.getLong(KEY_MAX_UPLOAD_SIZE_BYTES)
            ?: config.managementStorageSettings.maxUploadSizeBytes
    }

    override fun getTotalStorageQuotaForOpenUserBytes(): Long? {
        val raw = settingsService.getString(KEY_TOTAL_STORAGE_QUOTA_FOR_OPEN_USER_BYTES)
        if (raw.isNullOrBlank()) {
            return config.managementStorageSettings.totalStorageQuotaForOpenUserBytes
        }
        return raw.toLongOrNull()
    }

    override fun getTotalStorageQuotaForManagementUserBytes(): Long? {
        val raw = settingsService.getString(KEY_TOTAL_STORAGE_QUOTA_FOR_MANAGEMENT_USER_BYTES)
        if (raw.isNullOrBlank()) {
            return config.managementStorageSettings.totalStorageQuotaForManagementUserBytes
        }
        return raw.toLongOrNull()
    }

    override fun getPresignedUrlExpirationSeconds(): Int {
        return settingsService.getInt(KEY_PRESIGNED_URL_EXPIRATION_SECONDS)
            ?: config.managementStorageSettings.presignedUrlExpirationSeconds
    }

    override fun getMaxPendingUploadsForOpenUser(): Int {
        return settingsService.getInt(KEY_MAX_PENDING_UPLOADS_FOR_OPEN_USER)
            ?: config.managementStorageSettings.maxPendingUploadsForOpenUser
    }

    override fun getMaxPendingUploadsForManagementUser(): Int {
        return settingsService.getInt(KEY_MAX_PENDING_UPLOADS_FOR_MANAGEMENT_USER)
            ?: config.managementStorageSettings.maxPendingUploadsForManagementUser
    }

    override fun getIsDeduplicationEnabled(): Boolean {
        return settingsService.getBoolean(KEY_IS_DEDUPLICATION_ENABLED)
            ?: config.managementStorageSettings.isDeduplicationEnabled
    }

    override fun getDeletedFilesCleanupIntervalSeconds(): Int {
        return settingsService.getInt(KEY_DELETED_FILES_CLEANUP_INTERVAL_SECONDS)
            ?: config.managementStorageSettings.deletedFilesCleanupIntervalSeconds
    }

    override suspend fun updateManagementStorageSettings(
        managementStorageSettings: ManagementStorageSettings
    ): AppResult<Unit> {
        val settingsToUpdate = listOf(
            SystemSetting(
                key = KEY_MIN_UPLOAD_SIZE_BYTES,
                value = "${managementStorageSettings.minUploadSizeBytes}",
                type = SettingType.LONG
            ),
            SystemSetting(
                key = KEY_MAX_UPLOAD_SIZE_BYTES,
                value = "${managementStorageSettings.maxUploadSizeBytes}",
                type = SettingType.LONG
            ),
            SystemSetting(
                key = KEY_TOTAL_STORAGE_QUOTA_FOR_OPEN_USER_BYTES,
                value = managementStorageSettings.totalStorageQuotaForOpenUserBytes?.toString() ?: "",
                type = SettingType.STRING
            ),
            SystemSetting(
                key = KEY_TOTAL_STORAGE_QUOTA_FOR_MANAGEMENT_USER_BYTES,
                value = managementStorageSettings.totalStorageQuotaForManagementUserBytes?.toString() ?: "",
                type = SettingType.STRING
            ),
            SystemSetting(
                key = KEY_PRESIGNED_URL_EXPIRATION_SECONDS,
                value = "${managementStorageSettings.presignedUrlExpirationSeconds}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_MAX_PENDING_UPLOADS_FOR_OPEN_USER,
                value = "${managementStorageSettings.maxPendingUploadsForOpenUser}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_MAX_PENDING_UPLOADS_FOR_MANAGEMENT_USER,
                value = "${managementStorageSettings.maxPendingUploadsForManagementUser}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_IS_DEDUPLICATION_ENABLED,
                value = "${managementStorageSettings.isDeduplicationEnabled}",
                type = SettingType.BOOLEAN
            ),
            SystemSetting(
                key = KEY_DELETED_FILES_CLEANUP_INTERVAL_SECONDS,
                value = "${managementStorageSettings.deletedFilesCleanupIntervalSeconds}",
                type = SettingType.INT
            )
        )
        return settingsService.updateSettings(settingsToUpdate).mapSuccess { }
    }

    override suspend fun resetManagementStorageSettings(): AppResult<ManagementStorageSettings> {
        val defaults = config.managementStorageSettings
        return updateManagementStorageSettings(defaults).mapSuccess { defaults }
    }

    companion object {
        const val KEY_MIN_UPLOAD_SIZE_BYTES = "storage.min_upload_size_bytes"
        const val KEY_MAX_UPLOAD_SIZE_BYTES = "storage.max_upload_size_bytes"
        const val KEY_TOTAL_STORAGE_QUOTA_FOR_OPEN_USER_BYTES = "storage.total_storage_quota_for_open_user_bytes"
        const val KEY_TOTAL_STORAGE_QUOTA_FOR_MANAGEMENT_USER_BYTES = "storage.total_storage_quota_for_management_user_bytes"
        const val KEY_PRESIGNED_URL_EXPIRATION_SECONDS = "storage.presigned_url_expiration_seconds"
        const val KEY_MAX_PENDING_UPLOADS_FOR_OPEN_USER = "storage.max_pending_uploads_for_open_user"
        const val KEY_MAX_PENDING_UPLOADS_FOR_MANAGEMENT_USER = "storage.max_pending_uploads_for_management_user"
        const val KEY_IS_DEDUPLICATION_ENABLED = "storage.is_deduplication_enabled"
        const val KEY_DELETED_FILES_CLEANUP_INTERVAL_SECONDS = "storage.deleted_files_cleanup_interval_seconds"
    }
}
