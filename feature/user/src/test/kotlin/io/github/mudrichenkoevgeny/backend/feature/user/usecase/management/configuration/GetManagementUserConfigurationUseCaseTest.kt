package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.configuration

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.ManagementSecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.ManagementGlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.configuration.ManagementUserConfiguration
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetManagementUserConfigurationUseCaseTest {

    private val globalSettingsProvider = mockk<GlobalSettingsProvider>()
    private val securitySettingsProvider = mockk<SecuritySettingsProvider>()
    private val authSettingsProvider = mockk<AuthSettingsProvider>()

    private val useCase = GetManagementUserConfigurationUseCase(
        globalSettingsProvider = globalSettingsProvider,
        securitySettingsProvider = securitySettingsProvider,
        authSettingsProvider = authSettingsProvider
    )

    @Test
    fun `successfully aggregates all management settings into management user configuration`() {
        val globalSettings = mockk<ManagementGlobalSettings>()
        val securitySettings = mockk<ManagementSecuritySettings>()
        val authSettings = mockk<ManagementAuthSettings>()

        every { globalSettingsProvider.getManagementGlobalSettings() } returns globalSettings
        every { securitySettingsProvider.getManagementSecuritySettings() } returns securitySettings
        every { authSettingsProvider.getManagementAuthSettings() } returns authSettings

        val result = useCase()

        val expectedConfiguration = ManagementUserConfiguration(
            managementGlobalSettings = globalSettings,
            managementSecuritySettings = securitySettings,
            managementAuthSettings = authSettings
        )

        assertEquals(AppResult.Success(expectedConfiguration), result)
    }
}