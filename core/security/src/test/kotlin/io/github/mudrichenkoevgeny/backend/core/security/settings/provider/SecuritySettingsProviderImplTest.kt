package io.github.mudrichenkoevgeny.backend.core.security.settings.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.SecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.otpconfirmation.toOtpConfirmationPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.passwordpolicy.toPasswordPolicyPayload
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SecuritySettingsProviderImplTest {

    private val settingsService = mockk<SystemSettingsService>()
    private val defaultPolicy = PasswordPolicy(minLength = 10, requireDigit = true)
    private val defaultOtpConfirmation = OtpConfirmation(
        retryAfterSeconds = 60,
        numberOfSymbols = 6,
        expirationSeconds = 300
    )

    private val config = SecurityConfig(
        authRealm = "test-realm",
        totpEncryptionSecret = "test-secret",
        recentAuthenticationValidityInSeconds = 30,
        recentAuthenticationValidityInSecondsForManagement = 60,
        passwordPolicy = defaultPolicy,
        otpConfirmation = defaultOtpConfirmation,
        mfaTokenExpirationSeconds = 120
    )

    private val provider = SecuritySettingsProviderImpl(settingsService, config)

    @BeforeEach
    fun resetSettingsServiceMocks() {
        clearMocks(settingsService, answers = true, recordedCalls = true, childMocks = false)
    }

    @Test
    fun `initialize registers all default security settings`() = runTest {
        coEvery {
            settingsService.registerDefault(any(), any(), any())
        } returns AppResult.Success(Unit)

        val result = provider.initialize()

        assertEquals(AppResult.Success(Unit), result)

        val expectedPolicyJson = FoundationJson.encodeToString(config.passwordPolicy.toPasswordPolicyPayload())
        val expectedOtpJson = FoundationJson.encodeToString(config.otpConfirmation.toOtpConfirmationPayload())

        coVerifyOrder {
            settingsService.registerDefault(
                key = "security.recent_authentication_validity_in_seconds",
                value = "30",
                type = SettingType.INT
            )
            settingsService.registerDefault(
                key = "security.recent_authentication_validity_in_seconds_for_management",
                value = "60",
                type = SettingType.INT
            )
            settingsService.registerDefault(
                key = "security.password_policy",
                value = expectedPolicyJson,
                type = SettingType.JSON
            )
            settingsService.registerDefault(
                key = "security.otp_confirmation",
                value = expectedOtpJson,
                type = SettingType.JSON
            )
            settingsService.registerDefault(
                key = "security.mfa_token_expiration_seconds",
                value = "120",
                type = SettingType.INT
            )
        }
    }

    @Test
    fun `getSettings returns stored values when present`() {
        val storedPolicy = PasswordPolicy(minLength = 20, requireSpecialChar = true)
        val storedOtp = OtpConfirmation(retryAfterSeconds = 10, numberOfSymbols = 4, expirationSeconds = 60)

        every { settingsService.getLong("security.recent_authentication_validity_in_seconds") } returns 99L
        every { settingsService.getLong("security.recent_authentication_validity_in_seconds_for_management") } returns 120L
        every { settingsService.getLong("security.mfa_token_expiration_seconds") } returns 300L
        stubGetJsonPasswordPolicyDeserializesTo(storedPolicy)
        stubGetJsonOtpConfirmationDeserializesTo(storedOtp)

        val result = provider.getSettings()

        assertEquals(99, result.recentAuthenticationValiditySeconds)
        assertEquals(storedPolicy, result.passwordPolicy)
        assertEquals(storedOtp, result.otpConfirmation)
    }

    @Test
    fun `getSettings falls back to config when keys missing`() {
        every { settingsService.getLong(any()) } returns null
        stubGetJsonPasswordPolicyReturnsNull()
        stubGetJsonOtpConfirmationReturnsNull()

        val result = provider.getSettings()

        assertEquals(30, result.recentAuthenticationValiditySeconds)
        assertEquals(60, result.recentAuthenticationValiditySecondsForManagement)
        assertEquals(defaultPolicy, result.passwordPolicy)
    }

    @Test
    fun `getPasswordPolicy returns stored policy when present`() {
        val storedPolicy = PasswordPolicy(minLength = 8, requireUpperCase = true)
        stubGetJsonPasswordPolicyDeserializesTo(storedPolicy)

        val policy = provider.getPasswordPolicy()

        assertEquals(storedPolicy, policy)
    }

    @Test
    fun `getPasswordPolicy falls back to config policy when setting is missing`() {
        stubGetJsonPasswordPolicyReturnsNull()

        val policy = provider.getPasswordPolicy()

        assertEquals(defaultPolicy, policy)
    }

    @Test
    fun `updateSecuritySettings returns success when all updates succeed`() = runTest {
        val newSettings = SecuritySettings(
            recentAuthenticationValiditySeconds = 45,
            recentAuthenticationValiditySecondsForManagement = 90,
            passwordPolicy = PasswordPolicy(minLength = 25),
            otpConfirmation = defaultOtpConfirmation,
            mfaTokenExpirationSeconds = 180
        )

        coEvery {
            settingsService.updateSetting(any(), any(), any())
        } returns AppResult.Success(mockk())

        val result = provider.updateSecuritySettings(newSettings)

        assertEquals(AppResult.Success(Unit), result)
        coVerifyOrder {
            settingsService.updateSetting("security.recent_authentication_validity_in_seconds", "45", SettingType.INT)
            settingsService.updateSetting("security.recent_authentication_validity_in_seconds_for_management", "90", SettingType.INT)
            settingsService.updateSetting("security.password_policy", any(), SettingType.JSON)
            settingsService.updateSetting("security.otp_confirmation", any(), SettingType.JSON)
            settingsService.updateSetting("security.mfa_token_expiration_seconds", "180", SettingType.INT)
        }
    }

    private fun stubGetJsonPasswordPolicyDeserializesTo(storedPolicy: PasswordPolicy) {
        every {
            settingsService.getJson(
                "security.password_policy",
                any<(String) -> PasswordPolicy>()
            )
        } answers {
            @Suppress("UNCHECKED_CAST")
            val deserializer = invocation.args[1] as (String) -> PasswordPolicy
            deserializer(FoundationJson.encodeToString(storedPolicy.toPasswordPolicyPayload()))
        }
    }

    private fun stubGetJsonOtpConfirmationDeserializesTo(storedOtp: OtpConfirmation) {
        every {
            settingsService.getJson(
                "security.otp_confirmation",
                any<(String) -> OtpConfirmation>()
            )
        } answers {
            @Suppress("UNCHECKED_CAST")
            val deserializer = invocation.args[1] as (String) -> OtpConfirmation
            deserializer(FoundationJson.encodeToString(storedOtp.toOtpConfirmationPayload()))
        }
    }

    private fun stubGetJsonPasswordPolicyReturnsNull() {
        every {
            settingsService.getJson("security.password_policy", any<(String) -> PasswordPolicy>())
        } returns null
    }

    private fun stubGetJsonOtpConfirmationReturnsNull() {
        every {
            settingsService.getJson("security.otp_confirmation", any<(String) -> OtpConfirmation>())
        } returns null
    }
}