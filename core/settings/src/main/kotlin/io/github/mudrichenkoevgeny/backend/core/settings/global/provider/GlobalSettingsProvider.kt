package io.github.mudrichenkoevgeny.backend.core.settings.global.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.GlobalSettings

/**
 * Provides access to global settings that are safe to expose to clients.
 *
 * Implementation is responsible for seeding defaults (typically from environment configuration) and
 * for persisting updates into DB-backed settings storage.
 */
interface GlobalSettingsProvider {
    /**
     * Seeds default values for missing settings.
     *
     * @return [AppResult.Success] when seeding completed, or [AppResult.Error] on failure
     */
    suspend fun initialize(): AppResult<Unit>

    /**
     * Returns the current global settings snapshot.
     */
    fun getSettings(): AppResult<GlobalSettings>

    /**
     * Persists all global settings fields (nullable values are stored as empty strings).
     */
    suspend fun updateGlobalSettings(globalSettings: GlobalSettings): AppResult<Unit>
}