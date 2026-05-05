package io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.AvailableAuthProviders
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
     * Returns current effective management settings.
     */
    fun getManagementAuthSettings(): ManagementAuthSettings

    /**
     * Returns settings safe to expose to unauthenticated clients.
     */
    fun getPublicAuthSettings(): PublicAuthSettings

    /**
     * Returns the list of enabled authentication providers.
     */
    fun getAvailableAuthProviders(): AvailableAuthProviders

    /**
     * Returns the maximum number of identifiers of any type allowed per account.
     */
    fun getMaxTotalIdentifiers(): Int

    /**
     * Returns the maximum number of email-based identifiers allowed per account.
     */
    fun getMaxEmailIdentifiers(): Int

    /**
     * Returns the maximum number of phone-based identifiers allowed per account.
     */
    fun getMaxPhoneIdentifiers(): Int

    /**
     * Returns the maximum number of identifiers allowed for each unique external provider.
     */
    fun getMaxIdentifiersPerExternalProvider(): Int

    /**
     * Returns the maximum number of active sessions allowed per account.
     */
    fun getMaxActiveSessions(): Int

    /**
     * Returns the validity window for new access tokens in seconds.
     */
    fun getAccessTokenExpirationSeconds(): Int

    /**
     * Returns the validity window for new refresh tokens in seconds.
     */
    fun getRefreshTokenExpirationSeconds(): Int

    /**
     * Returns the delay in seconds between scheduling an account for deletion
     * and its permanent removal from the system.
     */
    fun getAccountDeletionDelaySeconds(): Int

    /**
     * Persists management auth settings.
     */
    suspend fun updateManagementAuthSettings(managementAuthSettings: ManagementAuthSettings): AppResult<Unit>
}
