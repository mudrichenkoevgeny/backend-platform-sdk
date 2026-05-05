package io.github.mudrichenkoevgeny.backend.core.settings.service

import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.BackgroundScope
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.github.mudrichenkoevgeny.backend.core.settings.manager.SystemSettingsManager
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingsUpdateEvent
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.uuid.Uuid

/**
 * Default [SystemSettingsService] implementation backed by [SystemSettingsManager] and an in-memory
 * cache.
 *
 * The cache is a best-effort view of the database:
 * - [initialize] loads all rows and populates the cache.
 * - [registerDefault] persists a key only if it is missing from the cache (after initialization).
 * - [updateSetting] persists the change and updates the cache entry.
 *
 * Typed getters read from the in-memory cache and perform parsing locally.
 */
@Singleton
class SystemSettingsServiceImpl @Inject constructor(
    private val systemSettingsManager: SystemSettingsManager,
    private val redisManager: RedisManager,
    @param:BackgroundScope private val scope: CoroutineScope
) : SystemSettingsService {

    private val isSubscribed = AtomicBoolean(false)
    private val instanceId = Uuid.random().toHexDashString()
    private val cache = ConcurrentHashMap<String, SystemSetting>()
    private val reloadMutex = Mutex()

    override suspend fun initialize(): AppResult<Unit> {
        val result = systemSettingsManager.getAllSettings()

        return when (result) {
            is AppResult.Success -> {
                result.data.forEach { cache[it.key] = it }

                if (isSubscribed.compareAndSet(expectedValue = false, newValue = true)) {
                    redisManager.subscribe(SETTINGS_CHANNEL) { message ->
                        val event = FoundationJson.decodeFromString<SettingsUpdateEvent>(message)
                        if (event.senderId != instanceId) {
                            scope.launch { reloadCache() }
                        }
                    }
                }

                AppResult.Success(Unit)
            }
            is AppResult.Error -> {
                AppResult.Error(result.error)
            }
        }
    }

    override suspend fun registerDefault(key: String, value: String, type: SettingType): AppResult<Unit> {
        if (cache.containsKey(key)) {
            return AppResult.Success(Unit)
        }

        val newSetting = SystemSetting(key = key, value = value, type = type)

        val result = systemSettingsManager.saveSetting(newSetting)

        return when (result) {
            is AppResult.Success -> {
                cache[key] = result.data

                broadcastUpdate()

                AppResult.Success(Unit)
            }
            is AppResult.Error -> {
                AppResult.Error(result.error)
            }
        }
    }

    override fun getString(key: String): String? = cache[key]?.value

    override fun getLong(key: String): Long? = cache[key]?.value?.toLongOrNull()

    override fun getInt(key: String): Int? = cache[key]?.value?.toIntOrNull()

    override fun getDouble(key: String): Double? = cache[key]?.value?.toDoubleOrNull()

    override fun getBoolean(key: String): Boolean? = cache[key]?.value?.toBooleanStrictOrNull()

    override fun <T> getJson(key: String, deserializer: (String) -> T): T? {
        val rawValue = cache[key]?.value ?: return null
        return try {
            deserializer(rawValue)
        } catch (_: Exception) {
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

        val result = systemSettingsManager.saveSetting(settingToSave)
        return when (result) {
            is AppResult.Success -> {
                cache[key] = result.data
                broadcastUpdate()
                AppResult.Success(result.data)
            }
            is AppResult.Error -> AppResult.Error(result.error)
        }
    }

    override suspend fun deleteSetting(key: String): AppResult<Unit> {
        return when (val result = systemSettingsManager.deleteSetting(key)) {
            is AppResult.Success -> {
                cache.remove(key)
                broadcastUpdate()
                AppResult.Success(Unit)
            }
            is AppResult.Error -> AppResult.Error(result.error)
        }
    }

    private suspend fun reloadCache() {
        reloadMutex.withLock {
            val result = systemSettingsManager.getAllSettings()
            if (result is AppResult.Success) {
                val settingsFromDb = result.data
                settingsFromDb.forEach { cache[it.key] = it }
                val keysInDb = settingsFromDb.map { it.key }.toSet()
                cache.keys.retainAll(keysInDb)
            }
        }
    }

    private fun broadcastUpdate() {
        val event = SettingsUpdateEvent(senderId = instanceId)
        scope.launch {
            redisManager.publish(SETTINGS_CHANNEL, FoundationJson.encodeToString(event))
        }
    }

    companion object {
        private const val SETTINGS_CHANNEL = "system_settings_updates"
    }
}