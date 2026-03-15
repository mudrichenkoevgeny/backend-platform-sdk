package io.github.mudrichenkoevgeny.backend.core.settings.manager

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting

interface SystemSettingsManager {
    suspend fun saveSetting(setting: SystemSetting): AppResult<SystemSetting>
    suspend fun getSettingByKey(key: String): AppResult<SystemSetting?>
    suspend fun getAllSettings(): AppResult<List<SystemSetting>>
}