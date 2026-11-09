package io.github.mudrichenkoevgeny.backend.core.security.config.envkeys

import io.github.mudrichenkoevgeny.backend.core.security.config.factory.SecurityConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig

/**
 * Environment variable keys used to build [SecurityConfig].
 *
 * These keys are read by [SecurityConfigFactoryImpl]. Consumers typically provide these variables
 * in the host application's deployment configuration.
 */
public object SecurityEnvKeys {
    public const val AUTH_REALM: String = "AUTH_REALM"
    public const val TOTP_ENCRYPTION_SECRET_FILE: String = "TOTP_ENCRYPTION_SECRET_FILE"
    public const val RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS: String = "RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS"
    public const val RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS_FOR_MANAGEMENT: String =
        "RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS_FOR_MANAGEMENT"
    public const val PASSWORD_POLICY_MIN_LENGTH: String = "PASSWORD_POLICY_MIN_LENGTH"
    public const val PASSWORD_POLICY_REQUIRE_LETTER: String = "PASSWORD_POLICY_REQUIRE_LETTER"
    public const val PASSWORD_POLICY_REQUIRE_UPPER_CASE: String = "PASSWORD_POLICY_REQUIRE_UPPER_CASE"
    public const val PASSWORD_POLICY_REQUIRE_LOWER_CASE: String = "PASSWORD_POLICY_REQUIRE_LOWER_CASE"
    public const val PASSWORD_POLICY_REQUIRE_DIGIT: String = "PASSWORD_POLICY_REQUIRE_DIGIT"
    public const val PASSWORD_POLICY_REQUIRE_SPECIAL_CHAR: String = "PASSWORD_POLICY_REQUIRE_SPECIAL_CHAR"
    public const val PASSWORD_POLICY_COMMON_PASSWORDS: String = "PASSWORD_POLICY_COMMON_PASSWORDS"
    public const val OTP_RETRY_AFTER_SECONDS: String = "OTP_RETRY_AFTER_SECONDS"
    public const val OTP_NUMBER_OF_SYMBOLS: String = "OTP_NUMBER_OF_SYMBOLS"
    public const val OTP_EXPIRATION_SECONDS: String = "OTP_EXPIRATION_SECONDS"
    public const val MFA_TOKEN_EXPIRATION_SECONDS: String = "MFA_TOKEN_EXPIRATION_SECONDS"
    public const val RATE_LIMIT_MAX_REQUESTS_PER_PERIOD: String = "RATE_LIMIT_MAX_REQUESTS_PER_PERIOD"
    public const val RATE_LIMIT_PERIOD_SECONDS: String = "RATE_LIMIT_PERIOD_SECONDS"
    public const val ACCOUNT_LOCKOUT_MAX_FAILED_PASSWORD_ATTEMPTS: String =
        "ACCOUNT_LOCKOUT_MAX_FAILED_PASSWORD_ATTEMPTS"
    public const val ACCOUNT_LOCKOUT_MAX_FAILED_OTP_ATTEMPTS: String = "ACCOUNT_LOCKOUT_MAX_FAILED_OTP_ATTEMPTS"
    public const val ACCOUNT_LOCKOUT_MAX_FAILED_TOTP_ATTEMPTS: String = "ACCOUNT_LOCKOUT_MAX_FAILED_TOTP_ATTEMPTS"
    public const val ACCOUNT_LOCKOUT_WINDOW_SECONDS: String = "ACCOUNT_LOCKOUT_WINDOW_SECONDS"
    public const val ACCOUNT_LOCKOUT_DURATION_SECONDS: String = "ACCOUNT_LOCKOUT_DURATION_SECONDS"
    public const val ACCOUNT_LOCKOUT_INDEFINITE_THRESHOLD: String = "ACCOUNT_LOCKOUT_INDEFINITE_THRESHOLD"
    public const val IS_SELF_SERVICE_UNLOCK_ENABLED: String = "IS_SELF_SERVICE_UNLOCK_ENABLED"
    public const val ACCOUNT_LOCKOUT_CHECK_INTERVAL_SECONDS: String = "ACCOUNT_LOCKOUT_CHECK_INTERVAL_SECONDS"
    public const val REFRESH_TOKEN_ROTATION_GRACE_PERIOD_SECONDS: String = "REFRESH_TOKEN_ROTATION_GRACE_PERIOD_SECONDS"

    public const val IS_IP_BLACKLIST_ENABLED_OPEN: String = "IS_IP_BLACKLIST_ENABLED_OPEN"
    public const val IP_BLACKLIST_OPEN: String = "IP_BLACKLIST_OPEN"
    public const val IS_IP_WHITELIST_ENABLED_OPEN: String = "IS_IP_WHITELIST_ENABLED_OPEN"
    public const val IP_WHITELIST_OPEN: String = "IP_WHITELIST_OPEN"

    public const val IS_IP_BLACKLIST_ENABLED_MANAGEMENT: String = "IS_IP_BLACKLIST_ENABLED_MANAGEMENT"
    public const val IP_BLACKLIST_MANAGEMENT: String = "IP_BLACKLIST_MANAGEMENT"
    public const val IS_IP_WHITELIST_ENABLED_MANAGEMENT: String = "IS_IP_WHITELIST_ENABLED_MANAGEMENT"
    public const val IP_WHITELIST_MANAGEMENT: String = "IP_WHITELIST_MANAGEMENT"
}
