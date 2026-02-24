package io.github.mudrichenkoevgeny.backend.core.settings.database.repository

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
interface SystemSettingRepository {
    suspend fun saveSetting(setting: SystemSetting): AppResult<SystemSetting>

    suspend fun getSettingByKey(key: String): AppResult<SystemSetting?>

    suspend fun getAllSettings(): AppResult<List<SystemSetting>>
}