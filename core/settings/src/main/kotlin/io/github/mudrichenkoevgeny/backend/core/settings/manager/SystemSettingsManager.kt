package io.github.mudrichenkoevgeny.backend.core.settings.manager

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting

/**
 * Transaction-bound access layer for working with persisted system settings.
 *
 * Unlike the repository, the manager is expected to run operations inside the configured database
 * context/transaction boundary.
 */
interface SystemSettingsManager {
    /** Saves (upserts) a setting. */
    suspend fun saveSetting(setting: SystemSetting): AppResult<SystemSetting>
    /** Loads a setting by key. */
    suspend fun getSettingByKey(key: String): AppResult<SystemSetting?>
    /** Loads all settings. */
    suspend fun getAllSettings(): AppResult<List<SystemSetting>>
    /** Deletes a setting by key. */
    suspend fun deleteSetting(key: String): AppResult<Unit>
}