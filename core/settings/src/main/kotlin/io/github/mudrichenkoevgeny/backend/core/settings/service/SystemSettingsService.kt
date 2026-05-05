package io.github.mudrichenkoevgeny.backend.core.settings.service

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting

/**
 * In-memory cached access to DB-backed system settings.
 *
 * Typical lifecycle:
 * - Call [initialize] once at startup to populate the cache from the database.
 * - Use typed getters to read values from memory.
 * - Use [registerDefault] to seed missing keys.
 * - Use [updateSetting] to persist changes and update the cache.
 */
interface SystemSettingsService {
    /**
     * Loads all settings from the database into the in-memory cache.
     *
     * @return [AppResult.Success] when the cache is populated, or [AppResult.Error] on failure
     */
    suspend fun initialize(): AppResult<Unit>

    /**
     * Persists a default value for [key] if it is missing from the cache.
     *
     * @param key unique setting key
     * @param value raw string value to store
     * @param type declares how [value] should be interpreted by consumers
     */
    suspend fun registerDefault(key: String, value: String, type: SettingType): AppResult<Unit>

    /** Returns the raw string value for [key], or `null` if missing. */
    fun getString(key: String): String?
    /** Parses the value as [Long], or returns `null` if missing/invalid. */
    fun getLong(key: String): Long?
    /** Parses the value as [Int], or returns `null` if missing/invalid. */
    fun getInt(key: String): Int?
    /** Parses the value as [Double], or returns `null` if missing/invalid. */
    fun getDouble(key: String): Double?
    /** Parses the value as a strict boolean, or returns `null` if missing/invalid. */
    fun getBoolean(key: String): Boolean?

    /**
     * Parses a JSON-encoded setting using the provided [deserializer].
     *
     * @param key unique setting key
     * @param deserializer mapping function from raw JSON string to a value
     * @return deserialized value or `null` if missing or parsing failed
     */
    fun <T> getJson(key: String, deserializer: (String) -> T): T?

    /**
     * Inserts or updates a setting and refreshes the cache entry.
     *
     * @param key unique setting key
     * @param value raw string value to store
     * @param type declares how [value] should be interpreted by consumers
     * @return [AppResult.Success] with the stored [SystemSetting], or [AppResult.Error]
     */
    suspend fun updateSetting(key: String, value: String, type: SettingType): AppResult<SystemSetting>

    /**
     * Deletes a setting from the database and removes it from the cache.
     *
     * @param key unique setting key to remove
     * @return [AppResult.Success] if deleted, or [AppResult.Error]
     */
    suspend fun deleteSetting(key: String): AppResult<Unit>
}