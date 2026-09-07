package io.github.mudrichenkoevgeny.backend.feature.settingsapi.usecase.management.globalsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.ManagementGlobalSettings
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetManagementGlobalSettingsUseCaseTest {

    private val globalSettingsProvider = mockk<GlobalSettingsProvider>()
    private val useCase = GetManagementGlobalSettingsUseCase(globalSettingsProvider)

    @Test
    fun `invoke - returns management global settings from provider`() {
        val expectedSettings = ManagementGlobalSettings(
            privacyPolicyUrl = "https://example.com/privacy",
            termsOfServiceUrl = "https://example.com/terms",
            contactSupportEmail = "support@example.com",
            maintenanceUntilEpochMillis = null,
            minSupportedAppVersions = emptyMap(),
            isTracingEnabled = false,
            isMetricsEnabled = false,
            isVerboseLoggingEnabled = false
        )

        every { globalSettingsProvider.getManagementGlobalSettings() } returns expectedSettings

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(expectedSettings, (result as AppResult.Success).data)
    }
}