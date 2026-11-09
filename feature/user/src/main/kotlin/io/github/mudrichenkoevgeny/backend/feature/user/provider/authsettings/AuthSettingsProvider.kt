package io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.AvailableAuthProviders
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.OpenAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.emailrestriction.EmailRestrictionPolicy

/**
 * Provides persisted authentication settings for the user feature.
 *
 * The provider is expected to:
 * - seed defaults on application startup ([initialize])
 * - expose a management snapshot ([getManagementAuthSettings])
 * - expose a public snapshot ([getOpenAuthSettings])
 * - persist updates from management flows ([updateManagementAuthSettings])
 */
public interface AuthSettingsProvider {
    /**
     * Registers default values for auth settings if they are missing.
     */
    public suspend fun initialize(): AppResult<Unit>

    /**
     * Returns current effective management settings.
     */
    public fun getManagementAuthSettings(): ManagementAuthSettings

    /**
     * Returns settings safe to expose to unauthenticated clients.
     */
    public fun getOpenAuthSettings(): OpenAuthSettings

    /**
     * Returns the list of enabled authentication providers.
     */
    public fun getAvailableAuthProviders(): AvailableAuthProviders

    /**
     * Returns the maximum number of identifiers of any type allowed per account.
     */
    public fun getMaxTotalIdentifiers(): Int

    /**
     * Returns the maximum number of email-based identifiers allowed per account.
     */
    public fun getMaxEmailIdentifiers(): Int

    /**
     * Returns the maximum number of phone-based identifiers allowed per account.
     */
    public fun getMaxPhoneIdentifiers(): Int

    /**
     * Returns the maximum number of identifiers allowed for each unique external provider.
     */
    public fun getMaxIdentifiersPerExternalProvider(): Int

    /**
     * Returns the minimum character length for user nicknames.
     */
    public fun getUserNicknameMinLength(): Int

    /**
     * Returns the maximum character length for user nicknames.
     */
    public fun getUserNicknameMaxLength(): Int

    /**
     * Returns the minimum character length for user display names.
     */
    public fun getUserDisplayNameMinLength(): Int

    /**
     * Returns the maximum character length for user display names.
     */
    public fun getUserDisplayNameMaxLength(): Int

    /**
     * Returns whether users are permitted to modify their nickname.
     */
    public fun getIsUserNicknameChangeAllowed(): Boolean

    /**
     * Returns the maximum number of active sessions allowed per open user account.
     */
    public fun getMaxActiveSessionsForOpenUser(): Int

    /**
     * Returns the maximum number of active sessions allowed per management user account.
     */
    public fun getMaxActiveSessionsForManagementUser(): Int

    /**
     * Returns the validity window for new access tokens in seconds.
     */
    public fun getAccessTokenExpirationSeconds(): Int

    /**
     * Returns the validity window for new refresh tokens in seconds.
     */
    public fun getRefreshTokenExpirationSeconds(): Int

    /**
     * Returns the grace period in seconds between scheduling an account for deletion
     * and its permanent removal from the system.
     */
    public fun getAccountDeletionGracePeriodSeconds(): Int

    /**
     * Returns the background worker check interval in seconds for processing accounts pending permanent deletion.
     */
    public fun getAccountDeletionCheckIntervalSeconds(): Int

    /**
     * Returns whether new user registrations are currently permitted.
     */
    public fun getIsRegistrationEnabled(): Boolean

    /**
     * Returns the open email restriction policy.
     */
    public fun getOpenEmailRestrictionPolicy(): EmailRestrictionPolicy

    /**
     * Returns the management email restriction policy.
     */
    public fun getManagementEmailRestrictionPolicy(): EmailRestrictionPolicy

    /**
     * Persists management auth settings.
     */
    public suspend fun updateManagementAuthSettings(managementAuthSettings: ManagementAuthSettings): AppResult<Unit>

    /**
     * Resets management auth settings to default configuration values.
     */
    public suspend fun resetManagementAuthSettings(): AppResult<ManagementAuthSettings>
}
