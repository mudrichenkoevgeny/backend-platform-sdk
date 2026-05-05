package io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.AvailableAuthProviders
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.coVerifyOrder
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

private const val KEY_AVAILABLE_PROVIDERS = "auth.available_auth_providers"
private const val KEY_MAX_TOTAL = "auth.max_total_identifiers"
private const val KEY_MAX_EMAIL = "auth.max_email_identifiers"
private const val KEY_MAX_PHONE = "auth.max_phone_identifiers"
private const val KEY_MAX_EXT = "auth.max_identifiers_per_external_provider"
private const val KEY_MAX_SESSIONS = "auth.max_active_sessions"
private const val KEY_ACCESS_EXP = "auth.access_token_expiration_seconds"
private const val KEY_REFRESH_EXP = "auth.refresh_token_expiration_seconds"
private const val KEY_DELETE_DELAY = "auth.account_deletion_delay_seconds"

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

        provider = AuthSettingsProviderImpl(settingsService, config)
    }

    @Test
    fun `should register all defaults during initialization`() = runTest {
        coEvery {
            settingsService.registerDefault(any(), any(), any())
        } returns AppResult.Success(Unit)

        val result = provider.initialize()

        assertTrue(result is AppResult.Success)
        coVerifyOrder {
            settingsService.registerDefault(KEY_AVAILABLE_PROVIDERS, any(), SettingType.JSON)
            settingsService.registerDefault(KEY_MAX_TOTAL, "5", SettingType.INT)
            settingsService.registerDefault(KEY_MAX_EMAIL, "1", SettingType.INT)
            settingsService.registerDefault(KEY_MAX_PHONE, "1", SettingType.INT)
            settingsService.registerDefault(KEY_MAX_EXT, "1", SettingType.INT)
            settingsService.registerDefault(KEY_MAX_SESSIONS, "3", SettingType.INT)
            settingsService.registerDefault(KEY_ACCESS_EXP, "3600", SettingType.INT)
            settingsService.registerDefault(KEY_REFRESH_EXP, "2592000", SettingType.INT)
            settingsService.registerDefault(KEY_DELETE_DELAY, "604800", SettingType.INT)
        }
    }

    @Test
    fun `should update all settings successfully`() = runTest {
        coEvery {
            settingsService.updateSetting(any(), any(), any())
        } answers {
            AppResult.Success(
                SystemSetting(
                    id = Uuid.random(),
                    key = firstArg(),
                    value = secondArg(),
                    type = thirdArg()
                )
            )
        }

        val result = provider.updateManagementAuthSettings(managementSettings)

        assertTrue(result is AppResult.Success)
        coVerifyOrder {
            settingsService.updateSetting(KEY_AVAILABLE_PROVIDERS, any(), SettingType.JSON)
            settingsService.updateSetting(KEY_MAX_TOTAL, "5", SettingType.INT)
            settingsService.updateSetting(KEY_MAX_EMAIL, "1", SettingType.INT)
            settingsService.updateSetting(KEY_MAX_PHONE, "1", SettingType.INT)
            settingsService.updateSetting(KEY_MAX_EXT, "1", SettingType.INT)
            settingsService.updateSetting(KEY_MAX_SESSIONS, "3", SettingType.INT)
            settingsService.updateSetting(KEY_ACCESS_EXP, "3600", SettingType.INT)
            settingsService.updateSetting(KEY_REFRESH_EXP, "2592000", SettingType.INT)
            settingsService.updateSetting(KEY_DELETE_DELAY, "604800", SettingType.INT)
        }
    }

    @Test
    fun `should return failure if update fails`() = runTest {
        coEvery {
            settingsService.updateSetting(KEY_AVAILABLE_PROVIDERS, any(), any())
        } returns AppResult.Error(mockk())

        val result = provider.updateManagementAuthSettings(managementSettings)

        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `should fallback to config when settings service is empty`() {
        every { settingsService.getJson<AvailableAuthProviders>(any(), any()) } returns null
        every { settingsService.getInt(any()) } returns null

        val result = provider.getManagementAuthSettings()

        assertEquals(availableAuthProviders, result.availableAuthProviders)
        assertEquals(5, result.maxTotalIdentifiers)
        assertEquals(3600, result.accessTokenExpirationSeconds)
    }

    @Test
    fun `should prefer service values over config`() {
        every { settingsService.getJson<AvailableAuthProviders>(KEY_AVAILABLE_PROVIDERS, any()) } returns null
        every { settingsService.getInt(KEY_MAX_TOTAL) } returns 100
        every { settingsService.getInt(not(KEY_MAX_TOTAL)) } returns null

        val result = provider.getManagementAuthSettings()

        assertEquals(100, result.maxTotalIdentifiers)
        assertEquals(3600, result.accessTokenExpirationSeconds)
    }
}