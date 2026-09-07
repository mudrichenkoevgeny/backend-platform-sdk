package io.github.mudrichenkoevgeny.backend.feature.settingsapi.usecase.open.globalsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.OpenGlobalSettings
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetOpenGlobalSettingsUseCaseTest {

    private val globalSettingsProvider = mockk<GlobalSettingsProvider>()
    private val useCase = GetOpenGlobalSettingsUseCase(globalSettingsProvider)

    @Test
    fun `invoke - returns open global settings from provider`() {
        val expectedSettings = OpenGlobalSettings(
            privacyPolicyUrl = "https://example.com/privacy",
            termsOfServiceUrl = "https://example.com/terms",
            contactSupportEmail = "support@example.com",
            maintenanceUntilEpochMillis = null,
            minSupportedAppVersions = emptyMap()
        )

        every { globalSettingsProvider.getOpenGlobalSettings() } returns expectedSettings

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(expectedSettings, (result as AppResult.Success).data)
    }
}