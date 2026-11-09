package io.github.mudrichenkoevgeny.backend.core.settings.manager

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting

/**
 * Transaction-bound access layer for working with persisted system settings.
 *
 * Unlike the repository, the manager is expected to run operations inside the configured database
 * context/transaction boundary.
 */
public interface SystemSettingsManager {
    /** Saves (upserts) multiple settings in a batch. */
    public suspend fun saveSettings(settings: List<SystemSetting>): AppResult<List<SystemSetting>>

    /** Saves (upserts) a setting. */
    public suspend fun saveSetting(setting: SystemSetting): AppResult<SystemSetting>

    /** Loads a setting by key. */
    public suspend fun getSettingByKey(key: String): AppResult<SystemSetting?>

    /** Loads all settings. */
    public suspend fun getAllSettings(): AppResult<List<SystemSetting>>

    /** Deletes a setting by key. */
    public suspend fun deleteSetting(key: String): AppResult<Unit>
}
