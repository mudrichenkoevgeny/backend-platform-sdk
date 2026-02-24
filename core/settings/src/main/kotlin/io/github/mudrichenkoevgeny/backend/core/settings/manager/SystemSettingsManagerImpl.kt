package io.github.mudrichenkoevgeny.backend.core.settings.manager

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.util.dbQuery
import io.github.mudrichenkoevgeny.backend.core.settings.database.repository.SystemSettingRepository
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Singleton
class SystemSettingsManagerImpl @Inject constructor(
    private val repository: SystemSettingRepository
) : SystemSettingsManager {

    override suspend fun saveSetting(setting: SystemSetting): AppResult<SystemSetting> = dbQuery {
        repository.saveSetting(setting)
    }

    override suspend fun getSettingByKey(key: String): AppResult<SystemSetting?> = dbQuery {
        repository.getSettingByKey(key)
    }

    override suspend fun getAllSettings(): AppResult<List<SystemSetting>> = dbQuery {
        repository.getAllSettings()
    }
}