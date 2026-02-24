package io.github.mudrichenkoevgeny.backend.core.settings.service

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.manager.SystemSettingsManager
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Singleton
class SystemSettingsServiceImpl @Inject constructor(
    private val manager: SystemSettingsManager
) : SystemSettingsService {

    private val cache = ConcurrentHashMap<String, SystemSetting>()

    override suspend fun initialize(): AppResult<Unit> {
        val result = manager.getAllSettings()

        return when (result) {
            is AppResult.Success -> {
                result.data.forEach { cache[it.key] = it }
                AppResult.Success(Unit)
            }
            is AppResult.Error -> {
                AppResult.Error(result.error)
            }
        }
    }

    override suspend fun registerDefault(key: String, value: String, type: SettingType): AppResult<Unit> {
        if (cache.containsKey(key)) return AppResult.Success(Unit)

        val newSetting = SystemSetting(key = key, value = value, type = type)

        val result = manager.saveSetting(newSetting)

        return when (result) {
            is AppResult.Success -> {
                cache[key] = result.data
                AppResult.Success(Unit)
            }
            is AppResult.Error -> {
                AppResult.Error(result.error)
            }
        }
    }

    override fun getString(key: String): String? = cache[key]?.value

    override fun getLong(key: String): Long? = cache[key]?.value?.toLongOrNull()

    override fun getDouble(key: String): Double? = cache[key]?.value?.toDoubleOrNull()

    override fun getBoolean(key: String): Boolean? = cache[key]?.value?.toBooleanStrictOrNull()

    override fun <T> getJson(key: String, deserializer: (String) -> T): T? {
        val rawValue = cache[key]?.value ?: return null
        return try {
            deserializer(rawValue)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateSetting(key: String, value: String, type: SettingType): AppResult<SystemSetting> {
        val existing = cache[key]
        val settingToSave = existing?.copy(value = value) ?: SystemSetting(
            key = key,
            value = value,
            type = type
        )

        return when (val result = manager.saveSetting(settingToSave)) {
            is AppResult.Success -> {
                cache[key] = result.data
                AppResult.Success(result.data)
            }
            is AppResult.Error -> {
                AppResult.Error(result.error)
            }
        }
    }
}