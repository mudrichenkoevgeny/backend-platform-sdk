package io.github.mudrichenkoevgeny.backend.core.security.settings.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.SecuritySettings

/**
 * Provides access to persisted security settings (e.g. via the system settings storage).
 *
 * The provider is expected to:
 * - seed defaults on application startup ([initialize])
 * - serve an effective [SecuritySettings] snapshot ([getSettings])
 * - provide a non-null password policy for validation flows ([getPasswordPolicy])
 */
interface SecuritySettingsProvider {
    /**
     * Registers default values for security settings if they are missing.
     */
    suspend fun initialize(): AppResult<Unit>

    /**
     * Returns current effective settings.
     */
    fun getSettings(): SecuritySettings

    /**
     * Returns the validity window (in seconds) for recent re-authentication in self-service flows.
     */
    fun getRecentAuthenticationValidityInSeconds(): Int

    /**
     * Returns the validity window (in seconds) for recent re-authentication in administrative/management flows.
     */
    fun getRecentAuthenticationValidityInSecondsForManagement(): Int

    /**
     * Returns the effective password policy.
     *
     * This method is intended for validation flows where a password policy must always be present.
     */
    fun getPasswordPolicy(): PasswordPolicy

    /**
     * Returns the effective OTP configuration.
     */
    fun getOtpConfirmation(): OtpConfirmation

    /**
     * Returns the expiration time (in seconds) for temporary MFA challenge tokens.
     */
    fun getMfaTokenExpirationSeconds(): Int

    /**
     * Updates the stored security settings including password policy, OTP, and expiration windows.
     */
    suspend fun updateSecuritySettings(securitySettings: SecuritySettings): AppResult<Unit>
}