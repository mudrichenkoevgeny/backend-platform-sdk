package io.github.mudrichenkoevgeny.backend.core.security.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.security.config.envkeys.SecurityEnvKeys
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.ManagementPasswordPolicy
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SecurityConfigFactoryImplTest {

    private val envReader = mockk<EnvReader>()
    private lateinit var factory: SecurityConfigFactoryImpl

    @BeforeEach
    fun setup() {
        factory = SecurityConfigFactoryImpl(envReader)

        every { envReader.getByKey(SecurityEnvKeys.TOTP_ENCRYPTION_SECRET_FILE) } returns "secret.file"
        every { envReader.readSecret("secret.file") } returns "top-secret-key"
        every { envReader.getByKey(SecurityEnvKeys.AUTH_REALM) } returns "TestRealm"

        every { envReader.getByKey(SecurityEnvKeys.OTP_RETRY_AFTER_SECONDS) } returns "60"
        every { envReader.getByKey(SecurityEnvKeys.OTP_NUMBER_OF_SYMBOLS) } returns "6"
        every { envReader.getByKey(SecurityEnvKeys.OTP_EXPIRATION_SECONDS) } returns "300"
        every { envReader.getByKey(SecurityEnvKeys.MFA_TOKEN_EXPIRATION_SECONDS) } returns "120"
    }

    @Test
    fun `create reads required keys and applies defaults for optional password policy`() {
        every { envReader.getByKey(SecurityEnvKeys.RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS) } returns "15"
        every { envReader.getByKey(SecurityEnvKeys.RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS_FOR_MANAGEMENT) } returns "45"

        every { envReader.getByKeyOrNull(any()) } returns null

        val config = factory.create()

        assertEquals("TestRealm", config.authRealm)
        assertEquals("top-secret-key", config.totpEncryptionSecret)
        assertEquals(15, config.managementSecuritySettings.recentAuthenticationValiditySecondsForOpenUser)
        assertEquals(45, config.managementSecuritySettings.recentAuthenticationValiditySecondsForManagementUser)

        assertEquals(ManagementPasswordPolicy.DEFAULT_MIN_LENGTH, config.managementSecuritySettings.passwordPolicy.minLength)
        assertTrue(config.managementSecuritySettings.passwordPolicy.requireLetter)
        assertFalse(config.managementSecuritySettings.passwordPolicy.requireUpperCase)

        assertEquals(6, config.managementSecuritySettings.otpConfirmation.numberOfSymbols)
        assertEquals(120, config.managementSecuritySettings.mfaTokenExpirationSeconds)
        assertEquals(SecurityConfig.DEFAULT_ACCOUNT_LOCKOUT_CHECK_INTERVAL_SECONDS, config.managementSecuritySettings.accountLockoutCheckIntervalSeconds)
        assertEquals(SecurityConfig.DEFAULT_REFRESH_TOKEN_ROTATION_GRACE_PERIOD_SECONDS, config.managementSecuritySettings.refreshTokenRotationGracePeriodSeconds)
    }

    @Test
    fun `create parses password policy values from env`() {
        every { envReader.getByKeyOrNull(any()) } returns null

        every { envReader.getByKey(SecurityEnvKeys.RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS) } returns "60"
        every { envReader.getByKey(SecurityEnvKeys.RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS_FOR_MANAGEMENT) } returns "90"

        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_MIN_LENGTH) } returns "12"
        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_LETTER) } returns "false"
        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_UPPER_CASE) } returns "true"
        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_LOWER_CASE) } returns "true"
        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_DIGIT) } returns "true"
        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_REQUIRE_SPECIAL_CHAR) } returns "true"
        every { envReader.getByKeyOrNull(SecurityEnvKeys.PASSWORD_POLICY_COMMON_PASSWORDS) } returns "pass123,  qwerty  , ,  letmein "
        every { envReader.getByKeyOrNull(SecurityEnvKeys.ACCOUNT_LOCKOUT_CHECK_INTERVAL_SECONDS) } returns "120"
        every { envReader.getByKeyOrNull(SecurityEnvKeys.REFRESH_TOKEN_ROTATION_GRACE_PERIOD_SECONDS) } returns "45"

        val config = factory.create()

        assertEquals(60, config.managementSecuritySettings.recentAuthenticationValiditySecondsForOpenUser)
        assertEquals(90, config.managementSecuritySettings.recentAuthenticationValiditySecondsForManagementUser)
        assertEquals(12, config.managementSecuritySettings.passwordPolicy.minLength)
        assertFalse(config.managementSecuritySettings.passwordPolicy.requireLetter)
        assertTrue(config.managementSecuritySettings.passwordPolicy.requireUpperCase)
        assertTrue(config.managementSecuritySettings.passwordPolicy.requireLowerCase)
        assertTrue(config.managementSecuritySettings.passwordPolicy.requireDigit)
        assertTrue(config.managementSecuritySettings.passwordPolicy.requireSpecialChar)
        assertEquals(setOf("pass123", "qwerty", "letmein"), config.managementSecuritySettings.passwordPolicy.commonPasswords)
        assertEquals(120, config.managementSecuritySettings.accountLockoutCheckIntervalSeconds)
        assertEquals(45, config.managementSecuritySettings.refreshTokenRotationGracePeriodSeconds)
    }
}
