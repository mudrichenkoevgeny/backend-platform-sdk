package io.github.mudrichenkoevgeny.backend.core.settings.global.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.GlobalSettings

/**
 * Provides access to global settings that are safe to expose to clients.
 *
 * Implementation is responsible for seeding defaults and for persisting updates
 * into the settings storage.
 */
interface GlobalSettingsProvider {
    /**
     * Seeds default values for missing global settings.
     */
    suspend fun initialize(): AppResult<Unit>

    /**
     * Returns the current global settings snapshot.
     */
    fun getSettings(): GlobalSettings

    /**
     * Returns the URL of the privacy policy page.
     */
    fun getPrivacyPolicyUrl(): String?

    /**
     * Returns the URL of the terms of service page.
     */
    fun getTermsOfServiceUrl(): String?

    /**
     * Returns the support contact email address.
     */
    fun getContactSupportEmail(): String?

    /**
     * Updates all global settings fields in the persistent storage.
     */
    suspend fun updateGlobalSettings(globalSettings: GlobalSettings): AppResult<Unit>
}