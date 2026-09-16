package io.github.mudrichenkoevgeny.backend.core.security.config.model

import io.github.mudrichenkoevgeny.backend.core.security.config.factory.SecurityConfigFactory
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.iprestriction.IpRestrictionPolicy
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
 * @property accountLockoutPolicy Default account lockout policy.
 * @property accountLockoutCheckIntervalSeconds Background worker check interval in seconds for processing expired account lockouts.
 * @property openIpRestrictionPolicy Default open IP restriction policy.
 * @property managementIpRestrictionPolicy Default management IP restriction policy.
 * @property mfaTokenExpirationSeconds Lifetime (in seconds) of the temporary MFA challenge token (mfaToken).
 * @property maxRequestsPerPeriod Maximum number of requests allowed per rate limit window.
 * @property rateLimitPeriodSeconds Rate limit time window duration in seconds.
 * @property refreshTokenRotationGracePeriodSeconds Grace period in seconds during refresh token rotation before triggering replay attack protection.
 */
data class SecurityConfig(
    val authRealm: String,
    val totpEncryptionSecret: String,
    val recentAuthenticationValidityInSeconds: Int,
    val recentAuthenticationValidityInSecondsForManagement: Int,
    val passwordPolicy: ManagementPasswordPolicy,
    val otpConfirmation: OtpConfirmation,
    val accountLockoutPolicy: AccountLockoutPolicy = DEFAULT_ACCOUNT_LOCKOUT_POLICY,
    val accountLockoutCheckIntervalSeconds: Int = DEFAULT_ACCOUNT_LOCKOUT_CHECK_INTERVAL_SECONDS,
    val openIpRestrictionPolicy: IpRestrictionPolicy = DEFAULT_IP_RESTRICTION_POLICY,
    val managementIpRestrictionPolicy: IpRestrictionPolicy = DEFAULT_IP_RESTRICTION_POLICY,
    val mfaTokenExpirationSeconds: Int,
    val maxRequestsPerPeriod: Int = DEFAULT_MAX_REQUESTS_PER_PERIOD,
    val rateLimitPeriodSeconds: Int = DEFAULT_RATE_LIMIT_PERIOD_SECONDS,
    val refreshTokenRotationGracePeriodSeconds: Int = DEFAULT_REFRESH_TOKEN_ROTATION_GRACE_PERIOD_SECONDS
) {
    companion object {
        const val DEFAULT_MAX_REQUESTS_PER_PERIOD = 100
        const val DEFAULT_RATE_LIMIT_PERIOD_SECONDS = 60
        const val DEFAULT_ACCOUNT_LOCKOUT_CHECK_INTERVAL_SECONDS = 60
        const val DEFAULT_REFRESH_TOKEN_ROTATION_GRACE_PERIOD_SECONDS = 30

        val DEFAULT_ACCOUNT_LOCKOUT_POLICY = AccountLockoutPolicy(
            maxFailedPasswordAttempts = 5,
            maxFailedOtpAttempts = 3,
            maxFailedTotpAttempts = 3,
            failedAttemptsWindowSeconds = 600,
            lockoutDurationSeconds = 1800,
            indefiniteLockoutThreshold = 3,
            isSelfServiceUnlockEnabled = true
        )

        val DEFAULT_IP_RESTRICTION_POLICY = IpRestrictionPolicy(
            isBlacklistEnabled = false,
            blacklist = emptyList(),
            isWhitelistEnabled = false,
            whitelist = emptyList()
        )
    }
}