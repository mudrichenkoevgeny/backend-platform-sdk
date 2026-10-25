package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.configuration

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.configuration.ManagementUserConfiguration
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.security.domain.model.securitysettings.createTestManagementSecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.settings.domain.model.globalsettings.createTestManagementGlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.storage.domain.model.storagesettings.createTestManagementStorageSettings
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.auth.settings.createTestManagementAuthSettings
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetManagementUserConfigurationUseCaseTest {

    private val globalSettingsProvider = mockk<GlobalSettingsProvider>()
    private val securitySettingsProvider = mockk<SecuritySettingsProvider>()
    private val storageSettingsProvider = mockk<StorageSettingsProvider>()
    private val authSettingsProvider = mockk<AuthSettingsProvider>()

    private val useCase = GetManagementUserConfigurationUseCase(
        globalSettingsProvider = globalSettingsProvider,
        securitySettingsProvider = securitySettingsProvider,
        storageSettingsProvider = storageSettingsProvider,
        authSettingsProvider = authSettingsProvider
    )

    @Test
    fun `successfully aggregates all management settings into management user configuration`() {
        val globalSettings = createTestManagementGlobalSettings()
        val securitySettings = createTestManagementSecuritySettings()
        val storageSettings = createTestManagementStorageSettings()
        val authSettings = createTestManagementAuthSettings()

        every { globalSettingsProvider.getManagementGlobalSettings() } returns globalSettings
        every { securitySettingsProvider.getManagementSecuritySettings() } returns securitySettings
        every { storageSettingsProvider.getManagementStorageSettings() } returns storageSettings
        every { authSettingsProvider.getManagementAuthSettings() } returns authSettings

        val result = useCase()

        val expectedConfiguration = ManagementUserConfiguration(
            managementGlobalSettings = globalSettings,
            managementSecuritySettings = securitySettings,
            managementStorageSettings = storageSettings,
            managementAuthSettings = authSettings
        )

        assertEquals(AppResult.Success(expectedConfiguration), result)
    }
}