package io.github.mudrichenkoevgeny.backend.core.settings.manager

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting

class TestSystemSettingsManager(
    private val getAllSettingsResult: AppResult<List<SystemSetting>> = AppResult.Success(emptyList()),
    private val saveSettingResult: AppResult<SystemSetting> = AppResult.Success(
        SystemSetting(key = "unused", value = "unused", type = SettingType.STRING)
    )
) : SystemSettingsManager {
    val saveCalls = mutableListOf<SystemSetting>()
    val deleteCalls = mutableListOf<String>()

    override suspend fun saveSettings(settings: List<SystemSetting>): AppResult<List<SystemSetting>> {
        saveCalls += settings
        return AppResult.Success(settings)
    }

    override suspend fun saveSetting(setting: SystemSetting): AppResult<SystemSetting> {
        saveCalls += setting
        return saveSettingResult
    }

    override suspend fun getSettingByKey(key: String): AppResult<SystemSetting?> = AppResult.Success(null)

    override suspend fun getAllSettings(): AppResult<List<SystemSetting>> = getAllSettingsResult

    override suspend fun deleteSetting(key: String): AppResult<Unit> {
        deleteCalls += key
        return AppResult.Success(Unit)
    }
}
