package io.github.mudrichenkoevgeny.backend.core.security.settings.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.config.model.createTestManagementSecuritySettings
import io.github.mudrichenkoevgeny.backend.core.security.config.model.createTestSecurityConfig
import io.github.mudrichenkoevgeny.backend.core.security.domain.model.otpconfirmation.createTestOtpConfirmation
import io.github.mudrichenkoevgeny.backend.core.security.domain.model.passwordpolicy.createTestManagementPasswordPolicy
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.ManagementPasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.otpconfirmation.toOtpConfirmationPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.passwordpolicy.toManagementPasswordPolicyPayload
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SecuritySettingsProviderImplTest {

    private val settingsService = mockk<SystemSettingsService>()
    private val defaultPolicy = createTestManagementPasswordPolicy(minLength = 10, requireDigit = true)
    private val defaultOtpConfirmation = createTestOtpConfirmation(
        retryAfterSeconds = 60,
        numberOfSymbols = 6,
        expirationSeconds = 300
    )

    private val config = createTestSecurityConfig(
        passwordPolicy = defaultPolicy,
        otpConfirmation = defaultOtpConfirmation
    )

    private val provider = SecuritySettingsProviderImpl(settingsService, config)

    @BeforeEach
    fun resetSettingsServiceMocks() {
        clearMocks(settingsService, answers = true, recordedCalls = true, childMocks = false)
    }

    @Test
    fun `initialize registers all default security settings`() = runTest {
        coEvery {
            settingsService.registerDefaults(any())
        } returns AppResult.Success(Unit)

        val result = provider.initialize()

        assertEquals(AppResult.Success(Unit), result)
    }

    @Test
    fun `getManagementSecuritySettings returns stored values when present`() {
        val storedPolicy = createTestManagementPasswordPolicy(minLength = 20, requireSpecialChar = true)
        val storedOtp = createTestOtpConfirmation(retryAfterSeconds = 10, numberOfSymbols = 4, expirationSeconds = 60)

        every { settingsService.getInt("security.recent_authentication_validity_in_seconds") } returns 99
        every { settingsService.getInt("security.recent_authentication_validity_in_seconds_for_management") } returns 120
        every { settingsService.getInt("security.mfa_token_expiration_seconds") } returns 300
        every { settingsService.getInt("security.max_requests_per_period") } returns 200
        every { settingsService.getInt("security.rate_limit_period_seconds") } returns 120
        every { settingsService.getJson<Any>(any(), any()) } returns null
        stubGetJsonPasswordPolicyDeserializesTo(storedPolicy)
        stubGetJsonOtpConfirmationDeserializesTo(storedOtp)

        val result = provider.getManagementSecuritySettings()

        assertEquals(99, result.recentAuthenticationValiditySecondsForOpenUser)
        assertEquals(120, result.recentAuthenticationValiditySecondsForManagementUser)
        assertEquals(storedPolicy, result.passwordPolicy)
        assertEquals(storedOtp, result.otpConfirmation)
        assertEquals(200, result.maxRequestsPerPeriod)
        assertEquals(120, result.rateLimitPeriodSeconds)
    }

    @Test
    fun `getManagementSecuritySettings falls back to config when keys missing`() {
        every { settingsService.getInt(any()) } returns null
        every { settingsService.getJson<Any>(any(), any()) } returns null

        val result = provider.getManagementSecuritySettings()

        assertEquals(30, result.recentAuthenticationValiditySecondsForOpenUser)
        assertEquals(60, result.recentAuthenticationValiditySecondsForManagementUser)
        assertEquals(defaultPolicy, result.passwordPolicy)
    }

    @Test
    fun `getManagementPasswordPolicy returns stored policy when present`() {
        val storedPolicy = createTestManagementPasswordPolicy(minLength = 8, requireUpperCase = true)
        stubGetJsonPasswordPolicyDeserializesTo(storedPolicy)

        val policy = provider.getManagementPasswordPolicy()

        assertEquals(storedPolicy, policy)
    }

    @Test
    fun `getManagementPasswordPolicy falls back to config policy when setting is missing`() {
        stubGetJsonPasswordPolicyReturnsNull()

        val policy = provider.getManagementPasswordPolicy()

        assertEquals(defaultPolicy, policy)
    }

    @Test
    fun `updateManagementSecuritySettings returns success when all updates succeed`() = runTest {
        val newSettings = createTestManagementSecuritySettings(
            recentAuthenticationValiditySecondsForOpenUser = 45,
            recentAuthenticationValiditySecondsForManagementUser = 90,
            passwordPolicy = createTestManagementPasswordPolicy(minLength = 25),
            otpConfirmation = defaultOtpConfirmation,
            mfaTokenExpirationSeconds = 180,
            maxRequestsPerPeriod = 150,
            rateLimitPeriodSeconds = 90
        )

        coEvery {
            settingsService.updateSettings(any())
        } returns AppResult.Success(emptyList())

        val result = provider.updateManagementSecuritySettings(newSettings)

        assertEquals(AppResult.Success(Unit), result)
    }

    private fun stubGetJsonPasswordPolicyDeserializesTo(storedPolicy: ManagementPasswordPolicy) {
        every {
            settingsService.getJson(
                "security.password_policy",
                any<(String) -> ManagementPasswordPolicy>()
            )
        } answers {
            @Suppress("UNCHECKED_CAST")
            val deserializer = invocation.args[1] as (String) -> ManagementPasswordPolicy
            deserializer(FoundationJson.encodeToString(storedPolicy.toManagementPasswordPolicyPayload()))
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
            settingsService.getJson("security.password_policy", any<(String) -> ManagementPasswordPolicy>())
        } returns null
    }

    private fun stubGetJsonOtpConfirmationReturnsNull() {
        every {
            settingsService.getJson("security.otp_confirmation", any<(String) -> OtpConfirmation>())
        } returns null
    }
}
