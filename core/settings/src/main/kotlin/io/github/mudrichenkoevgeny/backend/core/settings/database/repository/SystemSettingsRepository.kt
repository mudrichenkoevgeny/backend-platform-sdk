package io.github.mudrichenkoevgeny.backend.core.settings.database.repository

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting

/**
 * Repository for CRUD-like operations over [SystemSetting] rows.
 *
 * This is a low-level persistence contract. Higher-level caching and typed access is implemented by
 * `SystemSettingsService`.
 */
interface SystemSettingRepository {
    /**
     * Inserts or updates a setting row.
     *
     * @param setting setting to persist
     * @return [AppResult.Success] with the persisted setting (same values) or [AppResult.Error]
     */
    suspend fun saveSetting(setting: SystemSetting): AppResult<SystemSetting>

    /**
     * Returns a setting by its unique [key].
     *
     * @param key unique setting key
     * @return [AppResult.Success] with the setting or `null` if missing
     */
    suspend fun getSettingByKey(key: String): AppResult<SystemSetting?>

    /**
     * Loads all settings from storage.
     *
     * @return [AppResult.Success] with a list of settings (possibly empty)
     */
    suspend fun getAllSettings(): AppResult<List<SystemSetting>>

    /**
     * Deletes a setting row by its unique [key].
     *
     * @param key unique setting key
     * @return [AppResult.Success] if the row was deleted or didn't exist, [AppResult.Error] on DB failure
     */
    suspend fun deleteSetting(key: String): AppResult<Unit>
}