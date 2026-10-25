package io.github.mudrichenkoevgeny.backend.core.storage.settings.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.storagesettings.ManagementStorageSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.storagesettings.OpenStorageSettings

/**
 * Provider contract for accessing and managing persisted storage configuration settings.
 */
interface StorageSettingsProvider {

    /**
     * Seeds initial default settings from configuration into system settings storage.
     *
     * @return [AppResult.Success] on completion.
     */
    suspend fun initialize(): AppResult<Unit>

    /**
     * Returns the current effective management storage settings.
     *
     * @return [ManagementStorageSettings] snapshot.
     */
    fun getManagementStorageSettings(): ManagementStorageSettings

    /**
     * Returns the current effective open storage settings.
     *
     * @return [OpenStorageSettings] snapshot.
     */
    fun getOpenStorageSettings(): OpenStorageSettings

    /**
     * Returns the minimum permitted file upload size in bytes.
     */
    fun getMinUploadSizeBytes(): Long

    /**
     * Returns the maximum permitted single file upload size in bytes.
     */
    fun getMaxUploadSizeBytes(): Long

    /**
     * Returns the cumulative storage quota in bytes for regular open users.
     */
    fun getTotalStorageQuotaForOpenUserBytes(): Long?

    /**
     * Returns the cumulative storage quota in bytes for staff/management users.
     */
    fun getTotalStorageQuotaForManagementUserBytes(): Long?

    /**
     * Returns the presigned URL expiration duration in seconds.
     */
    fun getPresignedUrlExpirationSeconds(): Int

    /**
     * Returns the maximum allowed concurrent unfinalized upload sessions for open users.
     */
    fun getMaxPendingUploadsForOpenUser(): Int

    /**
     * Returns the maximum allowed concurrent unfinalized upload sessions for management users.
     */
    fun getMaxPendingUploadsForManagementUser(): Int

    /**
     * Returns whether Content-Addressable Storage (CAS) file deduplication is enabled.
     */
    fun getIsDeduplicationEnabled(): Boolean

    /**
     * Returns the interval in seconds for background cleanup of deleted files.
     */
    fun getDeletedFilesCleanupIntervalSeconds(): Int

    /**
     * Updates platform-wide management storage settings.
     *
     * @param managementStorageSettings New [ManagementStorageSettings] to persist.
     * @return [AppResult.Success] on completion.
     */
    suspend fun updateManagementStorageSettings(
        managementStorageSettings: ManagementStorageSettings
    ): AppResult<Unit>

    /**
     * Resets platform-wide management storage settings to default configuration values.
     *
     * @return [AppResult.Success] containing default [ManagementStorageSettings].
     */
    suspend fun resetManagementStorageSettings(): AppResult<ManagementStorageSettings>
}
