package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.configuration

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.OpenSecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.OpenGlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.OpenAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.configuration.OpenUserConfiguration
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetOpenUserConfigurationUseCaseTest {

    private val globalSettingsProvider = mockk<GlobalSettingsProvider>()
    private val securitySettingsProvider = mockk<SecuritySettingsProvider>()
    private val authSettingsProvider = mockk<AuthSettingsProvider>()

    private val useCase = GetOpenUserConfigurationUseCase(
        globalSettingsProvider = globalSettingsProvider,
        securitySettingsProvider = securitySettingsProvider,
        authSettingsProvider = authSettingsProvider
    )

    @Test
    fun `successfully aggregates all settings into user configuration`() {
        val globalSettings = mockk<OpenGlobalSettings>()
        val securitySettings = mockk<OpenSecuritySettings>()
        val authSettings = mockk<OpenAuthSettings>()

        every { globalSettingsProvider.getOpenGlobalSettings() } returns globalSettings
        every { securitySettingsProvider.getOpenSecuritySettings() } returns securitySettings
        every { authSettingsProvider.getOpenAuthSettings() } returns authSettings

        val result = useCase()

        val expectedConfiguration = OpenUserConfiguration(
            openGlobalSettings = globalSettings,
            openSecuritySettings = securitySettings,
            openAuthSettings = authSettings
        )

        assertEquals(AppResult.Success(expectedConfiguration), result)
    }
}