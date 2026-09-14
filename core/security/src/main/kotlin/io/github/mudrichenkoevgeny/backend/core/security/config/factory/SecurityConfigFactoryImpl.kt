package io.github.mudrichenkoevgeny.backend.core.security.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.common.config.env.getStringList
import io.github.mudrichenkoevgeny.backend.core.security.config.envkeys.SecurityEnvKeys
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.iprestriction.IpRestrictionPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.ManagementPasswordPolicy
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [SecurityConfigFactory] implementation that reads configuration from the environment.
 *
 * Expected variables are declared in [SecurityEnvKeys]. If an optional variable (such as password policy rules
 * or rate limit parameters) is missing, the implementation falls back to default values.
 */
@Singleton
class SecurityConfigFactoryImpl @Inject constructor(
    private val envReader: EnvReader
): SecurityConfigFactory {

    override fun create(): SecurityConfig {
        val totpEncryptionSecretFile = envReader.getByKey(SecurityEnvKeys.TOTP_ENCRYPTION_SECRET_FILE)

        val authRealm = envReader.getByKey(SecurityEnvKeys.AUTH_REALM)
        val totpEncryptionSecret = envReader.readSecret(totpEncryptionSecretFile)

        val recentAuthenticationValidityInSeconds = envReader
            .getByKey(SecurityEnvKeys.RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS).toInt()

        val recentAuthenticationValidityInSecondsForManagement = envReader
            .getByKey(SecurityEnvKeys.RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS_FOR_MANAGEMENT).toInt()

        val minLength = envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_MIN_LENGTH)
            ?.toInt() ?: ManagementPasswordPolicy.DEFAULT_MIN_LENGTH

        val requireLetter = envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_LETTER)
            ?.toBoolean() ?: true

        val requireUpperCase = envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_UPPER_CASE)
            ?.toBoolean() ?: false

        val requireLowerCase = envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_LOWER_CASE)
            ?.toBoolean() ?: false

        val requireDigit = envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_DIGIT)
            ?.toBoolean() ?: false

        val requireSpecialChar = envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_SPECIAL_CHAR)
            ?.toBoolean() ?: false

        val commonPasswords = envReader
            .getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_COMMON_PASSWORDS)
            ?.split(",")
            ?.asSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet() ?: ManagementPasswordPolicy.DEFAULT_COMMON_PASSWORDS

        val otpRetryAfterSeconds = envReader.getByKey(SecurityEnvKeys.OTP_RETRY_AFTER_SECONDS).toInt()
        val otpNumberOfSymbols = envReader.getByKey(SecurityEnvKeys.OTP_NUMBER_OF_SYMBOLS).toInt()
        val otpExpirationSeconds = envReader.getByKey(SecurityEnvKeys.OTP_EXPIRATION_SECONDS).toInt()

        val mfaTokenExpirationSeconds = envReader.getByKey(SecurityEnvKeys.MFA_TOKEN_EXPIRATION_SECONDS).toInt()

        val maxRequestsPerPeriod = envReader.getByKeyOrNull(SecurityEnvKeys.RATE_LIMIT_MAX_REQUESTS_PER_PERIOD)
            ?.toInt() ?: SecurityConfig.DEFAULT_MAX_REQUESTS_PER_PERIOD

        val rateLimitPeriodSeconds = envReader.getByKeyOrNull(SecurityEnvKeys.RATE_LIMIT_PERIOD_SECONDS)
            ?.toInt() ?: SecurityConfig.DEFAULT_RATE_LIMIT_PERIOD_SECONDS

        val maxFailedPasswordAttempts = envReader.getByKeyOrNull(SecurityEnvKeys.ACCOUNT_LOCKOUT_MAX_FAILED_PASSWORD_ATTEMPTS)?.toInt()
            ?: SecurityConfig.DEFAULT_ACCOUNT_LOCKOUT_POLICY.maxFailedPasswordAttempts
        val maxFailedOtpAttempts = envReader.getByKeyOrNull(SecurityEnvKeys.ACCOUNT_LOCKOUT_MAX_FAILED_OTP_ATTEMPTS)?.toInt()
            ?: SecurityConfig.DEFAULT_ACCOUNT_LOCKOUT_POLICY.maxFailedOtpAttempts
        val maxFailedTotpAttempts = envReader.getByKeyOrNull(SecurityEnvKeys.ACCOUNT_LOCKOUT_MAX_FAILED_TOTP_ATTEMPTS)?.toInt()
            ?: SecurityConfig.DEFAULT_ACCOUNT_LOCKOUT_POLICY.maxFailedTotpAttempts
        val failedAttemptsWindowSeconds = envReader.getByKeyOrNull(SecurityEnvKeys.ACCOUNT_LOCKOUT_WINDOW_SECONDS)?.toInt()
            ?: SecurityConfig.DEFAULT_ACCOUNT_LOCKOUT_POLICY.failedAttemptsWindowSeconds
        val lockoutDurationSeconds = envReader.getByKeyOrNull(SecurityEnvKeys.ACCOUNT_LOCKOUT_DURATION_SECONDS)?.toInt()
            ?: SecurityConfig.DEFAULT_ACCOUNT_LOCKOUT_POLICY.lockoutDurationSeconds
        val permanentLockoutThreshold = envReader.getByKeyOrNull(SecurityEnvKeys.ACCOUNT_LOCKOUT_PERMANENT_THRESHOLD)?.toInt()
            ?: SecurityConfig.DEFAULT_ACCOUNT_LOCKOUT_POLICY.permanentLockoutThreshold
        val isSelfServiceUnlockEnabled = envReader.getByKeyOrNull(SecurityEnvKeys.IS_SELF_SERVICE_UNLOCK_ENABLED)?.toBooleanStrictOrNull()
            ?: SecurityConfig.DEFAULT_ACCOUNT_LOCKOUT_POLICY.isSelfServiceUnlockEnabled

        val accountLockoutPolicy = AccountLockoutPolicy(
            maxFailedPasswordAttempts = maxFailedPasswordAttempts,
            maxFailedOtpAttempts = maxFailedOtpAttempts,
            maxFailedTotpAttempts = maxFailedTotpAttempts,
            failedAttemptsWindowSeconds = failedAttemptsWindowSeconds,
            lockoutDurationSeconds = lockoutDurationSeconds,
            permanentLockoutThreshold = permanentLockoutThreshold,
            isSelfServiceUnlockEnabled = isSelfServiceUnlockEnabled
        )

        val isOpenIpBlacklistEnabled = envReader.getByKeyOrNull(SecurityEnvKeys.IS_IP_BLACKLIST_ENABLED_OPEN)?.toBooleanStrictOrNull() ?: false
        val openIpBlacklist = envReader.getStringList(SecurityEnvKeys.IP_BLACKLIST_OPEN)
        val isOpenIpWhitelistEnabled = envReader.getByKeyOrNull(SecurityEnvKeys.IS_IP_WHITELIST_ENABLED_OPEN)?.toBooleanStrictOrNull() ?: false
        val openIpWhitelist = envReader.getStringList(SecurityEnvKeys.IP_WHITELIST_OPEN)

        val openIpRestrictionPolicy = IpRestrictionPolicy(
            isBlacklistEnabled = isOpenIpBlacklistEnabled,
            blacklist = openIpBlacklist,
            isWhitelistEnabled = isOpenIpWhitelistEnabled,
            whitelist = openIpWhitelist
        )

        val isManagementIpBlacklistEnabled = envReader.getByKeyOrNull(SecurityEnvKeys.IS_IP_BLACKLIST_ENABLED_MANAGEMENT)?.toBooleanStrictOrNull() ?: false
        val managementIpBlacklist = envReader.getStringList(SecurityEnvKeys.IP_BLACKLIST_MANAGEMENT)
        val isManagementIpWhitelistEnabled = envReader.getByKeyOrNull(SecurityEnvKeys.IS_IP_WHITELIST_ENABLED_MANAGEMENT)?.toBooleanStrictOrNull() ?: false
        val managementIpWhitelist = envReader.getStringList(SecurityEnvKeys.IP_WHITELIST_MANAGEMENT)

        val managementIpRestrictionPolicy = IpRestrictionPolicy(
            isBlacklistEnabled = isManagementIpBlacklistEnabled,
            blacklist = managementIpBlacklist,
            isWhitelistEnabled = isManagementIpWhitelistEnabled,
            whitelist = managementIpWhitelist
        )

        val passwordPolicy = ManagementPasswordPolicy(
            minLength = minLength,
            requireLetter = requireLetter,
            requireUpperCase = requireUpperCase,
            requireLowerCase = requireLowerCase,
            requireDigit = requireDigit,
            requireSpecialChar = requireSpecialChar,
            commonPasswords = commonPasswords
        )

        val otpConfirmation = OtpConfirmation(
            retryAfterSeconds = otpRetryAfterSeconds,
            numberOfSymbols = otpNumberOfSymbols,
            expirationSeconds = otpExpirationSeconds
        )

        return SecurityConfig(
            authRealm = authRealm,
            totpEncryptionSecret = totpEncryptionSecret,
            recentAuthenticationValidityInSeconds = recentAuthenticationValidityInSeconds,
            recentAuthenticationValidityInSecondsForManagement = recentAuthenticationValidityInSecondsForManagement,
            passwordPolicy = passwordPolicy,
            otpConfirmation = otpConfirmation,
            accountLockoutPolicy = accountLockoutPolicy,
            openIpRestrictionPolicy = openIpRestrictionPolicy,
            managementIpRestrictionPolicy = managementIpRestrictionPolicy,
            mfaTokenExpirationSeconds = mfaTokenExpirationSeconds,
            maxRequestsPerPeriod = maxRequestsPerPeriod,
            rateLimitPeriodSeconds = rateLimitPeriodSeconds
        )
    }
}
