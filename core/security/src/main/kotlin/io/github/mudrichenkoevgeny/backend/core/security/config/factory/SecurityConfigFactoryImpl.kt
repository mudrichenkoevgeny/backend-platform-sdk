package io.github.mudrichenkoevgeny.backend.core.security.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.security.config.envkeys.SecurityEnvKeys
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicy
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [SecurityConfigFactory] implementation that reads configuration from the environment.
 *
 * Expected variables are declared in [SecurityEnvKeys]. If a password policy-related variable is
 * missing, the implementation falls back to [PasswordPolicy] defaults.
 */
@Singleton
class SecurityConfigFactoryImpl @Inject constructor(
    private val envReader: EnvReader
): SecurityConfigFactory {

    override fun create(): SecurityConfig {
        // secret files
        val totpEncryptionSecretFile = envReader.getByKey(SecurityEnvKeys.TOTP_ENCRYPTION_SECRET_FILE)

        // env
        val authRealm = envReader.getByKey(SecurityEnvKeys.AUTH_REALM)
        val totpEncryptionSecret = envReader.readSecret(totpEncryptionSecretFile)

        val recentAuthenticationValidityInSeconds = envReader
            .getByKey(SecurityEnvKeys.RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS).toInt()

        val recentAuthenticationValidityInSecondsForManagement = envReader
            .getByKey(SecurityEnvKeys.RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS_FOR_MANAGEMENT).toInt()

        val minLength = envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_MIN_LENGTH)
            ?.toInt() ?: PasswordPolicy.DEFAULT_MIN_LENGTH

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
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet() ?: PasswordPolicy.DEFAULT_COMMON_PASSWORDS

        val otpRetryAfterSeconds = envReader.getByKey(SecurityEnvKeys.OTP_RETRY_AFTER_SECONDS).toInt()
        val otpNumberOfSymbols = envReader.getByKey(SecurityEnvKeys.OTP_NUMBER_OF_SYMBOLS).toInt()
        val otpExpirationSeconds = envReader.getByKey(SecurityEnvKeys.OTP_EXPIRATION_SECONDS).toInt()

        val mfaTokenExpirationSeconds = envReader.getByKey(SecurityEnvKeys.MFA_TOKEN_EXPIRATION_SECONDS).toInt()

        val passwordPolicy = PasswordPolicy(
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
            mfaTokenExpirationSeconds = mfaTokenExpirationSeconds
        )
    }
}