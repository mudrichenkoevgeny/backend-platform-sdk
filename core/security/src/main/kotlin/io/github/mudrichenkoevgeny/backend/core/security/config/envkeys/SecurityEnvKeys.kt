package io.github.mudrichenkoevgeny.backend.core.security.config.envkeys

import io.github.mudrichenkoevgeny.backend.core.security.config.factory.SecurityConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig

/**
 * Environment variable keys used to build [SecurityConfig].
 *
 * These keys are read by [SecurityConfigFactoryImpl]. Consumers typically provide these variables
 * in the host application's deployment configuration.
 */
object SecurityEnvKeys {
    const val AUTH_REALM = "AUTH_REALM"
    const val TOTP_ENCRYPTION_SECRET_FILE = "TOTP_ENCRYPTION_SECRET_FILE"
    const val RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS = "RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS"
    const val RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS_FOR_MANAGEMENT =
        "RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS_FOR_MANAGEMENT"
    const val PASSWORD_POLICY_MIN_LENGTH = "PASSWORD_POLICY_MIN_LENGTH"
    const val PASSWORD_POLICY_REQUIRE_LETTER = "PASSWORD_POLICY_REQUIRE_LETTER"
    const val PASSWORD_POLICY_REQUIRE_UPPER_CASE = "PASSWORD_POLICY_REQUIRE_UPPER_CASE"
    const val PASSWORD_POLICY_REQUIRE_LOWER_CASE = "PASSWORD_POLICY_REQUIRE_LOWER_CASE"
    const val PASSWORD_POLICY_REQUIRE_DIGIT = "PASSWORD_POLICY_REQUIRE_DIGIT"
    const val PASSWORD_POLICY_REQUIRE_SPECIAL_CHAR = "PASSWORD_POLICY_REQUIRE_SPECIAL_CHAR"
    const val PASSWORD_POLICY_COMMON_PASSWORDS = "PASSWORD_POLICY_COMMON_PASSWORDS"
    const val OTP_RETRY_AFTER_SECONDS = "OTP_RETRY_AFTER_SECONDS"
    const val OTP_NUMBER_OF_SYMBOLS = "OTP_NUMBER_OF_SYMBOLS"
    const val OTP_EXPIRATION_SECONDS = "OTP_EXPIRATION_SECONDS"
    const val MFA_TOKEN_EXPIRATION_SECONDS = "MFA_TOKEN_EXPIRATION_SECONDS"
}