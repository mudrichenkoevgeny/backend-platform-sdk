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
data class SecurityConfig(
    val authRealm: String,
    val totpEncryptionSecret: String,
    val managementSecuritySettings: ManagementSecuritySettings
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
