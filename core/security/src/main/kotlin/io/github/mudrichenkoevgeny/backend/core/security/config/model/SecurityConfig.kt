package io.github.mudrichenkoevgeny.backend.core.security.config.model

import io.github.mudrichenkoevgeny.backend.core.security.config.factory.SecurityConfigFactory
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.ManagementPasswordPolicy

/**
 * Runtime security configuration resolved at application startup.
 *
 * This config is produced by [SecurityConfigFactory] (typically backed by environment variables) and
 * injected where security-related decisions are made (e.g. authentication confirmation freshness,
 * default password policy).
 *
 * @param authRealm authentication realm used by the server
 * @param totpEncryptionSecret symmetric key used to encrypt and decrypt user TOTP secrets before database persistence
 * @property recentAuthenticationValidityInSeconds How long (in seconds) a recent re-auth remains acceptable
 * for sensitive **self-service** actions.
 * @property recentAuthenticationValidityInSecondsForManagement How long (in seconds) a recent re-auth remains
 * acceptable for sensitive **management** actions.
 * @property passwordPolicy Default password policy used as a fallback when no system setting
 * overrides it.
 * @property otpConfirmation Global parameters for handling one-time confirmation codes.
 * @property mfaTokenExpirationSeconds Lifetime (in seconds) of the temporary MFA challenge token (mfaToken).
 * @property maxRequestsPerPeriod Maximum number of requests allowed per rate limit window.
 * @property rateLimitPeriodSeconds Rate limit time window duration in seconds.
 */
data class SecurityConfig(
    val authRealm: String,
    val totpEncryptionSecret: String,
    val recentAuthenticationValidityInSeconds: Int,
    val recentAuthenticationValidityInSecondsForManagement: Int,
    val passwordPolicy: ManagementPasswordPolicy,
    val otpConfirmation: OtpConfirmation,
    val mfaTokenExpirationSeconds: Int,
    val maxRequestsPerPeriod: Int = DEFAULT_MAX_REQUESTS_PER_PERIOD,
    val rateLimitPeriodSeconds: Int = DEFAULT_RATE_LIMIT_PERIOD_SECONDS
) {
    companion object {
        const val DEFAULT_MAX_REQUESTS_PER_PERIOD = 100
        const val DEFAULT_RATE_LIMIT_PERIOD_SECONDS = 60
    }
}