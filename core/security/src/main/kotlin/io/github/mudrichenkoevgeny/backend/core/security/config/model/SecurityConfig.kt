package io.github.mudrichenkoevgeny.backend.core.security.config.model

import io.github.mudrichenkoevgeny.backend.core.security.config.factory.SecurityConfigFactory
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.iprestriction.IpRestrictionPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.ManagementSecuritySettings

/**
 * Runtime security configuration resolved at application startup.
 *
 * This config is produced by [SecurityConfigFactory] (typically backed by environment variables) and
 * injected where security-related decisions are made (e.g. authentication confirmation freshness,
 * default password policy).
 *
 * @param authRealm authentication realm used by the server
 * @param totpEncryptionSecret symmetric key used to encrypt and decrypt user TOTP secrets before database persistence
 * @param managementSecuritySettings default management security settings configuration
 */
public data class SecurityConfig(
    public val authRealm: String,
    public val totpEncryptionSecret: String,
    public val managementSecuritySettings: ManagementSecuritySettings
) {
    public companion object {
        public const val DEFAULT_MAX_REQUESTS_PER_PERIOD: Int = 100
        public const val DEFAULT_RATE_LIMIT_PERIOD_SECONDS: Int = 60
        public const val DEFAULT_ACCOUNT_LOCKOUT_CHECK_INTERVAL_SECONDS: Int = 60
        public const val DEFAULT_REFRESH_TOKEN_ROTATION_GRACE_PERIOD_SECONDS: Int = 30
        public const val DEFAULT_FAILED_ATTEMPTS_WINDOW_SECONDS: Int = 600
        public const val DEFAULT_LOCKOUT_DURATION_SECONDS: Int = 1800

        public val DEFAULT_ACCOUNT_LOCKOUT_POLICY: AccountLockoutPolicy = AccountLockoutPolicy(
            maxFailedPasswordAttempts = 5,
            maxFailedOtpAttempts = 3,
            maxFailedTotpAttempts = 3,
            failedAttemptsWindowSeconds = DEFAULT_FAILED_ATTEMPTS_WINDOW_SECONDS,
            lockoutDurationSeconds = DEFAULT_LOCKOUT_DURATION_SECONDS,
            indefiniteLockoutThreshold = 3,
            isSelfServiceUnlockEnabled = true
        )

        public val DEFAULT_IP_RESTRICTION_POLICY: IpRestrictionPolicy = IpRestrictionPolicy(
            isBlacklistEnabled = false,
            blacklist = emptyList(),
            isWhitelistEnabled = false,
            whitelist = emptyList()
        )
    }
}
