package io.github.mudrichenkoevgeny.backend.core.security.settings.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.model.PasswordPolicy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SecuritySettingsProviderImplTest {

    private val settingsService = mockk<SystemSettingsService>()
    private val defaultPolicy = PasswordPolicy(minLength = 10, requireDigit = true)
    private val config = SecurityConfig(
        authenticationConfirmationValidityMinutes = 30,
        passwordPolicy = defaultPolicy
    )
    private val provider = SecuritySettingsProviderImpl(settingsService, config)

    @Test
    fun `initialize registers default password policy json`() = runTest {
        coEvery {
            settingsService.registerDefault(
                key = any(),
                value = any(),
                type = any()
            )
        } returns AppResult.Success(Unit)

        val result = provider.initialize()

        assertEquals(AppResult.Success(Unit), result)
        val expectedJson = FoundationJson.encodeToString(defaultPolicy)
        coVerify {
            settingsService.registerDefault(
                key = "security.password_policy",
                value = expectedJson,
                type = SettingType.JSON
            )
        }
    }

    @Test
    fun `getSettings returns stored policy when present`() {
        val storedPolicy = PasswordPolicy(minLength = 20, requireSpecialChar = true)
        every { settingsService.getJson<PasswordPolicy>("security.password_policy", any()) } returns storedPolicy

        val result = provider.getSettings() as AppResult.Success

        assertEquals(storedPolicy, result.data.passwordPolicy)
    }

    @Test
    fun `getSettings falls back to config policy when setting is missing`() {
        every { settingsService.getJson<PasswordPolicy>("security.password_policy", any()) } returns null

        val result = provider.getSettings() as AppResult.Success

        assertEquals(defaultPolicy, result.data.passwordPolicy)
    }

    @Test
    fun `requirePasswordPolicy returns stored policy when present`() {
        val storedPolicy = PasswordPolicy(minLength = 8, requireUpperCase = true)
        every { settingsService.getJson<PasswordPolicy>("security.password_policy", any()) } returns storedPolicy

        val policy = provider.requirePasswordPolicy()

        assertEquals(storedPolicy, policy)
    }

    @Test
    fun `requirePasswordPolicy falls back to config policy when setting is missing`() {
        every { settingsService.getJson<PasswordPolicy>("security.password_policy", any()) } returns null

        val policy = provider.requirePasswordPolicy()

        assertEquals(defaultPolicy, policy)
    }

    @Test
    fun `updatePasswordPolicy returns success when system setting update succeeds`() = runTest {
        val newPolicy = PasswordPolicy(minLength = 25)
        val jsonValue = FoundationJson.encodeToString(newPolicy)

        coEvery { settingsService.updateSetting("security.password_policy", jsonValue, SettingType.JSON) } returns
            AppResult.Success(SystemSetting(key = "security.password_policy", value = jsonValue, type = SettingType.JSON))

        val result = provider.updatePasswordPolicy(newPolicy)

        assertEquals(AppResult.Success(Unit), result)
    }
}

