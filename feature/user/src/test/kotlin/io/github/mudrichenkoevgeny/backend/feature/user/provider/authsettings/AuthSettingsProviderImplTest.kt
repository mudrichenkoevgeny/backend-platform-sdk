package io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthSettings
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AvailableAuthProviders
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthSettingsProviderImplTest {

    private val settingsService: SystemSettingsService = mockk()
    private val defaultAvailable = AvailableAuthProviders(
        primary = listOf(UserAuthProvider.EMAIL),
        secondary = listOf(UserAuthProvider.GOOGLE)
    )
    private val provider = AuthSettingsProviderImpl(
        settingsService = settingsService,
        authSettings = AuthSettings(availableAuthProviders = defaultAvailable)
    )

    @Test
    fun `initialize registers default available providers setting as JSON`() = runBlocking {
        coEvery {
            settingsService.registerDefault(
                key = KEY_AVAILABLE_AUTH_PROVIDERS,
                value = any(),
                type = SettingType.JSON
            )
        } returns AppResult.Success(Unit)

        val result = provider.initialize()

        assertTrue(result is AppResult.Success)

        val expectedJson = FoundationJson.encodeToString(defaultAvailable)
        coVerify(exactly = 1) {
            settingsService.registerDefault(
                key = KEY_AVAILABLE_AUTH_PROVIDERS,
                value = expectedJson,
                type = SettingType.JSON
            )
        }
    }

    @Test
    fun `getSettings returns value from system settings`() {
        val storedAvailable = AvailableAuthProviders(
            primary = listOf(UserAuthProvider.PHONE),
            secondary = emptyList()
        )

        every {
            settingsService.getJson(KEY_AVAILABLE_AUTH_PROVIDERS, any<(String) -> AvailableAuthProviders>())
        } returns storedAvailable

        val result = provider.getSettings()

        assertTrue(result is AppResult.Success)
        assertEquals(storedAvailable, (result as AppResult.Success).data.availableAuthProviders)
    }

    @Test
    fun `getSettings falls back to empty providers when setting missing`() {
        every {
            settingsService.getJson(KEY_AVAILABLE_AUTH_PROVIDERS, any<(String) -> AvailableAuthProviders>())
        } returns null

        val result = provider.getSettings()

        assertTrue(result is AppResult.Success)
        val available = (result as AppResult.Success).data.availableAuthProviders
        assertEquals(emptyList<UserAuthProvider>(), available.primary)
        assertEquals(emptyList<UserAuthProvider>(), available.secondary)
    }

    @Test
    fun `updateAvailableAuthProviders persists JSON and returns unit on success`() = runBlocking {
        val updateAvailable = AvailableAuthProviders(
            primary = listOf(UserAuthProvider.EMAIL, UserAuthProvider.GOOGLE),
            secondary = listOf(UserAuthProvider.PHONE)
        )

        val expectedJson = FoundationJson.encodeToString(updateAvailable)

        coEvery {
            settingsService.updateSetting(
                key = KEY_AVAILABLE_AUTH_PROVIDERS,
                value = expectedJson,
                type = SettingType.JSON
            )
        } returns AppResult.Success(
            SystemSetting(
                key = KEY_AVAILABLE_AUTH_PROVIDERS,
                value = expectedJson,
                type = SettingType.JSON
            )
        )

        val result = provider.updateAvailableAuthProviders(updateAvailable)

        assertTrue(result is AppResult.Success)
        assertEquals(Unit, (result as AppResult.Success).data)
    }

    @Test
    fun `updateAvailableAuthProviders propagates errors from settings service`() = runBlocking {
        val updateAvailable = AvailableAuthProviders(
            primary = emptyList(),
            secondary = emptyList()
        )
        val expectedJson = FoundationJson.encodeToString(updateAvailable)

        val error = CommonError.Unknown(message = "db down")
        coEvery {
            settingsService.updateSetting(
                key = KEY_AVAILABLE_AUTH_PROVIDERS,
                value = expectedJson,
                type = SettingType.JSON
            )
        } returns AppResult.Error(error)

        val result = provider.updateAvailableAuthProviders(updateAvailable)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)
    }

    private companion object {
        const val KEY_AVAILABLE_AUTH_PROVIDERS = "auth.available_auth_providers"
    }
}

