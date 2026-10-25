package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.configuration

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.configuration.OpenUserConfiguration
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.security.domain.model.securitysettings.createTestOpenSecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.settings.domain.model.globalsettings.createTestOpenGlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.storage.domain.model.storagesettings.createTestOpenStorageSettings
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.auth.settings.createTestOpenAuthSettings
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetOpenUserConfigurationUseCaseTest {

    private val globalSettingsProvider = mockk<GlobalSettingsProvider>()
    private val securitySettingsProvider = mockk<SecuritySettingsProvider>()
    private val storageSettingsProvider = mockk<StorageSettingsProvider>()
    private val authSettingsProvider = mockk<AuthSettingsProvider>()

    private val useCase = GetOpenUserConfigurationUseCase(
        globalSettingsProvider = globalSettingsProvider,
        securitySettingsProvider = securitySettingsProvider,
        storageSettingsProvider = storageSettingsProvider,
        authSettingsProvider = authSettingsProvider
    )

    @Test
    fun `successfully aggregates all settings into user configuration`() {
        val globalSettings = createTestOpenGlobalSettings()
        val securitySettings = createTestOpenSecuritySettings()
        val storageSettings = createTestOpenStorageSettings()
        val authSettings = createTestOpenAuthSettings()

        every { globalSettingsProvider.getOpenGlobalSettings() } returns globalSettings
        every { securitySettingsProvider.getOpenSecuritySettings() } returns securitySettings
        every { storageSettingsProvider.getOpenStorageSettings() } returns storageSettings
        every { authSettingsProvider.getOpenAuthSettings() } returns authSettings

        val result = useCase()

        val expectedConfiguration = OpenUserConfiguration(
            openGlobalSettings = globalSettings,
            openSecuritySettings = securitySettings,
            openStorageSettings = storageSettings,
            openAuthSettings = authSettings
        )

        assertEquals(AppResult.Success(expectedConfiguration), result)
    }
}