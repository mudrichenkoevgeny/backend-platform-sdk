package io.github.mudrichenkoevgeny.backend.core.settings.global.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.model.GlobalSettings

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

    /** Updates the stored privacy policy URL value. */
    suspend fun updatePrivacyPolicyUrl(url: String): AppResult<Unit>
    /** Updates the stored terms of service URL value. */
    suspend fun updateTermsOfServiceUrl(url: String): AppResult<Unit>
    /** Updates the stored support email value. */
    suspend fun updateContactSupportEmail(email: String): AppResult<Unit>
}