package io.github.mudrichenkoevgeny.backend.core.security.settings.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.iprestriction.IpRestrictionPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.ManagementPasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.OpenPasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.ManagementSecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.OpenSecuritySettings

/**
 * Provides access to persisted security settings (e.g. via the system settings storage).
 *
 * The provider is expected to:
 * - seed defaults on application startup ([initialize])
 * - serve an effective [ManagementSecuritySettings] snapshot ([getManagementSecuritySettings])
 * - provide a non-null password policy for validation flows ([getManagementPasswordPolicy])
 */
interface SecuritySettingsProvider {
    /**
     * Registers default values for security settings if they are missing.
     */
    suspend fun initialize(): AppResult<Unit>

    /**
     * Returns current effective management settings.
     */
    fun getManagementSecuritySettings(): ManagementSecuritySettings

    /**
     * Returns current effective open security settings.
     */
    fun getOpenSecuritySettings(): OpenSecuritySettings

    /**
     * Returns the validity window (in seconds) for recent re-authentication in self-service flows.
     */
    fun getRecentAuthenticationValidityInSeconds(): Int

    /**
     * Returns the validity window (in seconds) for recent re-authentication in administrative/management flows.
     */
    fun getRecentAuthenticationValidityInSecondsForManagement(): Int

    /**
     * Returns the effective management password policy.
     *
     * This method is intended for validation flows where a password policy must always be present.
     */
    fun getManagementPasswordPolicy(): ManagementPasswordPolicy

    /**
     * Returns the effective open password policy.
     */
    fun getOpenPasswordPolicy(): OpenPasswordPolicy

    /**
     * Returns the effective OTP configuration.
     */
    fun getOtpConfirmation(): OtpConfirmation

    /**
     * Returns the effective account lockout policy.
     */
    fun getAccountLockoutPolicy(): AccountLockoutPolicy

    /**
     * Returns the background worker check interval in seconds for processing expired account lockouts.
     */
    fun getAccountLockoutCheckIntervalSeconds(): Int

    /**
     * Returns the effective open IP restriction policy.
     */
    fun getOpenIpRestrictionPolicy(): IpRestrictionPolicy

    /**
     * Returns the effective management IP restriction policy.
     */
    fun getManagementIpRestrictionPolicy(): IpRestrictionPolicy

    /**
     * Returns the expiration time (in seconds) for temporary MFA challenge tokens.
     */
    fun getMfaTokenExpirationSeconds(): Int

    /**
     * Returns the maximum number of requests allowed per rate limit period.
     */
    fun getMaxRequestsPerPeriod(): Int

    /**
     * Returns the rate limit time window in seconds.
     */
    fun getRateLimitPeriodSeconds(): Int

    /**
     * Returns the grace period in seconds during refresh token rotation before triggering replay attack protection.
     */
    fun getRefreshTokenRotationGracePeriodSeconds(): Int

    /**
     * Updates the stored security settings including password policy, OTP, and expiration windows.
     */
    suspend fun updateManagementSecuritySettings(managementSecuritySettings: ManagementSecuritySettings): AppResult<Unit>
}