package io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.AvailableAuthProviders
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthSettingsProviderImplTest {

    private val settingsService = mockk<SystemSettingsService>()
    private val config = mockk<UserConfig>()
    private val managementSettings = mockk<ManagementAuthSettings>()

    private val availableAuthProviders = AvailableAuthProviders(
        primary = listOf(UserAuthProvider.EMAIL),
        secondary = listOf(UserAuthProvider.GOOGLE)
    )

    private lateinit var provider: AuthSettingsProviderImpl

    @BeforeEach
    fun setUp() {
        every { config.managementAuthSettings } returns managementSettings
        every { managementSettings.availableAuthProviders } returns availableAuthProviders
        every { managementSettings.maxTotalIdentifiers } returns 5
        every { managementSettings.maxEmailIdentifiers } returns 1
        every { managementSettings.maxPhoneIdentifiers } returns 1
        every { managementSettings.maxIdentifiersPerExternalProvider } returns 1
        every { managementSettings.maxActiveSessions } returns 3
        every { managementSettings.accessTokenExpirationSeconds } returns 3600
        every { managementSettings.refreshTokenExpirationSeconds } returns 2592000
        every { managementSettings.accountDeletionDelaySeconds } returns 604800
        every { managementSettings.isRegistrationEnabled } returns true

        provider = AuthSettingsProviderImpl(settingsService, config)
    }

    @Test
    fun `should register all defaults during initialization`() = runTest {
        coEvery {
            settingsService.registerDefaults(any())
        } returns AppResult.Success(Unit)

        val result = provider.initialize()

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 1) {
            settingsService.registerDefaults(match { list ->
                list.size == 10
            })
        }
    }

    @Test
    fun `should update all settings successfully`() = runTest {
        coEvery {
            settingsService.updateSettings(any())
        } returns AppResult.Success(emptyList())

        val result = provider.updateManagementAuthSettings(managementSettings)

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 1) {
            settingsService.updateSettings(match { list ->
                list.size == 10
            })
        }
    }

    @Test
    fun `should return failure if update fails`() = runTest {
        coEvery {
            settingsService.updateSettings(any())
        } returns AppResult.Error(mockk())

        val result = provider.updateManagementAuthSettings(managementSettings)

        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `should fallback to config when settings service is empty`() {
        every { settingsService.getJson<AvailableAuthProviders>(any(), any()) } returns null
        every { settingsService.getInt(any()) } returns null
        every { settingsService.getBoolean(any()) } returns null

        val result = provider.getManagementAuthSettings()

        assertEquals(availableAuthProviders, result.availableAuthProviders)
        assertEquals(5, result.maxTotalIdentifiers)
        assertEquals(3600, result.accessTokenExpirationSeconds)
        assertEquals(true, result.isRegistrationEnabled)
    }

    @Test
    fun `should prefer service values over config`() {
        every { settingsService.getJson<AvailableAuthProviders>(any(), any()) } returns null
        every { settingsService.getInt("auth.max_total_identifiers") } returns 100
        every { settingsService.getInt(not("auth.max_total_identifiers")) } returns null
        every { settingsService.getBoolean(any()) } returns null

        val result = provider.getManagementAuthSettings()

        assertEquals(100, result.maxTotalIdentifiers)
        assertEquals(3600, result.accessTokenExpirationSeconds)
    }
}
