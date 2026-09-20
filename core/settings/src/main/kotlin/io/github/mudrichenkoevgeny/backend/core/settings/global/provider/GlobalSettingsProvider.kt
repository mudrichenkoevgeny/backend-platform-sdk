package io.github.mudrichenkoevgeny.backend.core.settings.global.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.ManagementGlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.OpenGlobalSettings

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
     * Returns current open global settings snapshot.
     */
    fun getOpenGlobalSettings(): OpenGlobalSettings

    /**
     * Returns current management global settings snapshot.
     */
    fun getManagementGlobalSettings(): ManagementGlobalSettings

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
     * Returns minimum supported app versions mapped by client type.
     */
    fun getMinSupportedAppVersions(): Map<ClientType, String>

    /**
     * Returns whether distributed tracing is enabled.
     */
    fun getIsTracingEnabled(): Boolean

    /**
     * Returns whether performance metrics collection is enabled.
     */
    fun getIsMetricsEnabled(): Boolean

    /**
     * Returns whether verbose logging is enabled.
     */
    fun getIsVerboseLoggingEnabled(): Boolean

    /**
     * Updates all global settings fields in the persistent storage.
     */
    suspend fun updateManagementGlobalSettings(managementGlobalSettings: ManagementGlobalSettings): AppResult<Unit>

    /**
     * Resets global settings to default configuration values.
     */
    suspend fun resetManagementGlobalSettings(): AppResult<ManagementGlobalSettings>
}