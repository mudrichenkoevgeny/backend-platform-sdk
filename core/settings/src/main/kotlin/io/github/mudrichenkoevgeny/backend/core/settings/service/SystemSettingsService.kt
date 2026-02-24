package io.github.mudrichenkoevgeny.backend.core.settings.service

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting

interface SystemSettingsService {
    suspend fun initialize(): AppResult<Unit>
    suspend fun registerDefault(key: String, value: String, type: SettingType): AppResult<Unit>

    fun getString(key: String): String?
    fun getLong(key: String): Long?
    fun getDouble(key: String): Double?
    fun getBoolean(key: String): Boolean?
    fun <T> getJson(key: String, deserializer: (String) -> T): T?

    suspend fun updateSetting(key: String, value: String, type: SettingType): AppResult<SystemSetting>
}