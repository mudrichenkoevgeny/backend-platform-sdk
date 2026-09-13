package io.github.mudrichenkoevgeny.backend.core.settings.service

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import kotlin.uuid.Uuid

data class RegisterDefaultCall(
    val key: String,
    val value: String,
    val type: SettingType
)

data class UpdateSettingCall(
    val key: String,
    val value: String,
    val type: SettingType
)

class RecordingSystemSettingsService(
    private val stringByKey: Map<String, String?> = emptyMap(),
    private val booleanByKey: Map<String, Boolean?> = emptyMap(),
    private val failUpdateForKey: String? = null,
    private val failUpdateError: AppError? = null
) : SystemSettingsService {
    val registerDefaultCalls = mutableListOf<RegisterDefaultCall>()
    val updateSettingCalls = mutableListOf<UpdateSettingCall>()

    override suspend fun initialize(): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun registerDefaults(settings: List<SystemSetting>): AppResult<Unit> {
        settings.forEach { registerDefaultCalls += RegisterDefaultCall(it.key, it.value, it.type) }
        return AppResult.Success(Unit)
    }

    override suspend fun registerDefault(key: String, value: String, type: SettingType): AppResult<Unit> {
        registerDefaultCalls += RegisterDefaultCall(key, value, type)
        return AppResult.Success(Unit)
    }

    override fun getString(key: String): String? = stringByKey[key]
    override fun getLong(key: String): Long? = null
    override fun getInt(key: String): Int? = null
    override fun getDouble(key: String): Double? = null
    override fun getBoolean(key: String): Boolean? = booleanByKey[key]
    override fun <T> getJson(key: String, deserializer: (String) -> T): T? = null

    override suspend fun updateSettings(settings: List<SystemSetting>): AppResult<List<SystemSetting>> {
        settings.forEach {
            updateSettingCalls += UpdateSettingCall(it.key, it.value, it.type)
            if (it.key == failUpdateForKey && failUpdateError != null) {
                return AppResult.Error(failUpdateError)
            }
        }
        return AppResult.Success(settings)
    }

    override suspend fun updateSetting(
        key: String,
        value: String,
        type: SettingType
    ): AppResult<SystemSetting> {
        updateSettingCalls += UpdateSettingCall(key, value, type)
        if (key == failUpdateForKey && failUpdateError != null) {
            return AppResult.Error(failUpdateError)
        }
        return AppResult.Success(
            SystemSetting(
                id = Uuid.random(),
                key = key,
                value = value,
                type = type
            )
        )
    }

    override suspend fun deleteSetting(key: String): AppResult<Unit> = AppResult.Success(Unit)
}
