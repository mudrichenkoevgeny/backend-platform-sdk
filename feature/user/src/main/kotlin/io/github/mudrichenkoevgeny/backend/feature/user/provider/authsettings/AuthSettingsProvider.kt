package io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.PublicAuthSettings

/**
 * Provides persisted authentication settings for the user feature.
 *
 * The provider is expected to:
 * - seed defaults on application startup ([initialize])
 * - expose a management snapshot ([getManagementAuthSettings])
 * - expose a public snapshot ([getPublicAuthSettings])
 * - persist updates from management flows ([updateManagementAuthSettings])
 */
interface AuthSettingsProvider {
    /**
     * Registers default values for auth settings if they are missing.
     */
    suspend fun initialize(): AppResult<Unit>

    /**
     * Returns current effective management settings (including token validity).
     */
    fun getManagementAuthSettings(): AppResult<ManagementAuthSettings>

    /**
     * Returns settings safe to expose to unauthenticated clients.
     */
    fun getPublicAuthSettings(): AppResult<PublicAuthSettings>

    /**
     * Persists management auth settings.
     */
    suspend fun updateManagementAuthSettings(managementAuthSettings: ManagementAuthSettings): AppResult<Unit>
}
