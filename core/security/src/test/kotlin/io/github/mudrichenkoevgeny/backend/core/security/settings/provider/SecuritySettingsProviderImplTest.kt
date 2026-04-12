package io.github.mudrichenkoevgeny.backend.core.security.settings.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.SecuritySettings
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
    private val config = SecurityConfig(
        recentAuthenticationValidityInMinutes = 30,
        passwordPolicy = defaultPolicy
    )
    private val provider = SecuritySettingsProviderImpl(settingsService, config)

    @BeforeEach
    fun resetSettingsServiceMocks() {
        clearMocks(settingsService, answers = true, recordedCalls = true, childMocks = false)
    }

    @Test
    fun `initialize registers recent auth validity then password policy json`() = runTest {
        coEvery {
            settingsService.registerDefault(
                key = any(),
                value = any(),
                type = any()
            )
        } returns AppResult.Success(Unit)

        val result = provider.initialize()

        assertEquals(AppResult.Success(Unit), result)
        val expectedPolicyJson = FoundationJson.encodeToString(config.passwordPolicy.toPasswordPolicyPayload())
        coVerifyOrder {
            settingsService.registerDefault(
                key = "security.recent_authentication_validity_in_minutes",
                value = "30",
                type = SettingType.LONG
            )
            settingsService.registerDefault(
                key = "security.password_policy",
                value = expectedPolicyJson,
                type = SettingType.JSON
            )
        }
    }

    @Test
    fun `getSettings returns stored values when present`() {
        val storedPolicy = PasswordPolicy(minLength = 20, requireSpecialChar = true)
        every { settingsService.getLong("security.recent_authentication_validity_in_minutes") } returns 99L
        stubGetJsonPasswordPolicyDeserializesTo(storedPolicy)

        val result = provider.getSettings() as AppResult.Success

        assertEquals(99L, result.data.recentAuthenticationValidityInMinutes)
        assertEquals(storedPolicy, result.data.passwordPolicy)
    }

    @Test
    fun `getSettings falls back to config when keys missing`() {
        every { settingsService.getLong("security.recent_authentication_validity_in_minutes") } returns null
        stubGetJsonPasswordPolicyReturnsNull()

        val result = provider.getSettings() as AppResult.Success

        assertEquals(30L, result.data.recentAuthenticationValidityInMinutes)
        assertEquals(defaultPolicy, result.data.passwordPolicy)
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
    fun `updateSecuritySettings returns success when both updates succeed`() = runTest {
        val newSettings = SecuritySettings(
            recentAuthenticationValidityInMinutes = 45L,
            passwordPolicy = PasswordPolicy(minLength = 25)
        )
        val policyJson = FoundationJson.encodeToString(newSettings.passwordPolicy)

        coEvery {
            settingsService.updateSetting(
                "security.recent_authentication_validity_in_minutes",
                "45",
                SettingType.LONG
            )
        } returns AppResult.Success(
            SystemSetting(
                key = "security.recent_authentication_validity_in_minutes",
                value = "45",
                type = SettingType.LONG
            )
        )
        coEvery {
            settingsService.updateSetting("security.password_policy", policyJson, SettingType.JSON)
        } returns AppResult.Success(
            SystemSetting(key = "security.password_policy", value = policyJson, type = SettingType.JSON)
        )

        val result = provider.updateSecuritySettings(newSettings)

        assertEquals(AppResult.Success(Unit), result)
        coVerifyOrder {
            settingsService.updateSetting(
                "security.recent_authentication_validity_in_minutes",
                "45",
                SettingType.LONG
            )
            settingsService.updateSetting("security.password_policy", policyJson, SettingType.JSON)
        }
    }

    private fun stubGetJsonPasswordPolicyDeserializesTo(storedPolicy: PasswordPolicy) {
        every {
            settingsService.getJson(
                "security.password_policy",
                any<(String) -> PasswordPolicy>(),
            )
        } answers {
            @Suppress("UNCHECKED_CAST")
            val deserializer = invocation.args[1] as (String) -> PasswordPolicy
            deserializer(FoundationJson.encodeToString(storedPolicy.toPasswordPolicyPayload()))
        }
    }

    private fun stubGetJsonPasswordPolicyReturnsNull() {
        every {
            settingsService.getJson(
                "security.password_policy",
                any<(String) -> PasswordPolicy>(),
            )
        } answers { null }
    }
}
