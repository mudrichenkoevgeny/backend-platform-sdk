package io.github.mudrichenkoevgeny.backend.core.security.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.security.config.envkeys.SecurityEnvKeys
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.model.PasswordPolicy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityConfigFactoryImpl @Inject constructor(
    private val envReader: EnvReader
): SecurityConfigFactory {

    override fun create(): SecurityConfig {
        val authenticationConfirmationValidityMinutes = envReader
            .getByKey(SecurityEnvKeys.AUTHENTICATION_CONFIRMATION_VALIDITY_MINUTES).toLong()

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

        val passwordPolicy = PasswordPolicy(
            minLength = minLength,
            requireLetter = requireLetter,
            requireUpperCase = requireUpperCase,
            requireLowerCase = requireLowerCase,
            requireDigit = requireDigit,
            requireSpecialChar = requireSpecialChar,
            commonPasswords = commonPasswords
        )

        return SecurityConfig(
            authenticationConfirmationValidityMinutes = authenticationConfirmationValidityMinutes,
            passwordPolicy = passwordPolicy
        )
    }
}