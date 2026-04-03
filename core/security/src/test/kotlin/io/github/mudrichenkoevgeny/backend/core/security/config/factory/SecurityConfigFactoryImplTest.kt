package io.github.mudrichenkoevgeny.backend.core.security.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.security.config.envkeys.SecurityEnvKeys
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.model.PasswordPolicy
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SecurityConfigFactoryImplTest {

    @Test
    fun `create reads required keys and applies defaults for optional password policy`() {
        val envReader = mockk<EnvReader>()
        every { envReader.getByKey(SecurityEnvKeys.AUTHENTICATION_CONFIRMATION_VALIDITY_MINUTES) } returns "15"

        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_MIN_LENGTH) } returns null
        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_LETTER) } returns null
        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_UPPER_CASE) } returns null
        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_LOWER_CASE) } returns null
        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_DIGIT) } returns null
        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_SPECIAL_CHAR) } returns null
        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_COMMON_PASSWORDS) } returns null

        val factory = SecurityConfigFactoryImpl(envReader)

        val config = factory.create()

        assertEquals(15L, config.recentAuthenticationValidityInMinutes)
        assertEquals(PasswordPolicy.DEFAULT_MIN_LENGTH, config.passwordPolicy.minLength)
        assertTrue(config.passwordPolicy.requireLetter)
        assertFalse(config.passwordPolicy.requireUpperCase)
        assertFalse(config.passwordPolicy.requireLowerCase)
        assertFalse(config.passwordPolicy.requireDigit)
        assertFalse(config.passwordPolicy.requireSpecialChar)
        assertEquals(PasswordPolicy.DEFAULT_COMMON_PASSWORDS, config.passwordPolicy.commonPasswords)
    }

    @Test
    fun `create parses password policy values from env`() {
        val envReader = mockk<EnvReader>()
        every { envReader.getByKey(SecurityEnvKeys.AUTHENTICATION_CONFIRMATION_VALIDITY_MINUTES) } returns "60"

        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_MIN_LENGTH) } returns "12"
        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_LETTER) } returns "false"
        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_UPPER_CASE) } returns "true"
        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_LOWER_CASE) } returns "true"
        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_DIGIT) } returns "true"
        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_SPECIAL_CHAR) } returns "true"
        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_COMMON_PASSWORDS) } returns "pass123,  qwerty  , ,  letmein "

        val factory = SecurityConfigFactoryImpl(envReader)

        val config = factory.create()

        assertEquals(60L, config.recentAuthenticationValidityInMinutes)
        assertEquals(12, config.passwordPolicy.minLength)
        assertFalse(config.passwordPolicy.requireLetter)
        assertTrue(config.passwordPolicy.requireUpperCase)
        assertTrue(config.passwordPolicy.requireLowerCase)
        assertTrue(config.passwordPolicy.requireDigit)
        assertTrue(config.passwordPolicy.requireSpecialChar)
        assertEquals(setOf("pass123", "qwerty", "letmein"), config.passwordPolicy.commonPasswords)
    }
}

