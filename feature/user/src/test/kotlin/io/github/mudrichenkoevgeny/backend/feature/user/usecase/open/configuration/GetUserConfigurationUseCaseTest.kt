package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.configuration

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.SecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.GlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.PublicAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.configuration.UserConfiguration
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetUserConfigurationUseCaseTest {

    private val globalSettingsProvider = mockk<GlobalSettingsProvider>()
    private val securitySettingsProvider = mockk<SecuritySettingsProvider>()
    private val authSettingsProvider = mockk<AuthSettingsProvider>()

    private val useCase = GetUserConfigurationUseCase(
        globalSettingsProvider = globalSettingsProvider,
        securitySettingsProvider = securitySettingsProvider,
        authSettingsProvider = authSettingsProvider
    )

    @Test
    fun `successfully aggregates all settings into user configuration`() {
        val globalSettings = mockk<GlobalSettings>()
        val securitySettings = mockk<SecuritySettings>()
        val authSettings = mockk<PublicAuthSettings>()

        every { globalSettingsProvider.getSettings() } returns globalSettings
        every { securitySettingsProvider.getSettings() } returns securitySettings
        every { authSettingsProvider.getPublicAuthSettings() } returns authSettings

        val result = useCase()

        val expectedConfiguration = UserConfiguration(
            globalSettings = globalSettings,
            securitySettings = securitySettings,
            authSettings = authSettings
        )

        assertEquals(AppResult.Success(expectedConfiguration), result)
    }
}