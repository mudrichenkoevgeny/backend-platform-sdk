package io.github.mudrichenkoevgeny.backend.feature.settingsapi.api.usecase.open.globalsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.GlobalSettings
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetGlobalSettingsUseCaseTest {

    private val globalSettingsProvider = mockk<GlobalSettingsProvider>()
    private val useCase = GetGlobalSettingsUseCase(globalSettingsProvider)

    @Test
    fun `invoke - returns global settings from provider`() {
        val expectedSettings = GlobalSettings(
            privacyPolicyUrl = "https://example.com/privacy",
            termsOfServiceUrl = "https://example.com/terms",
            contactSupportEmail = "support@example.com"
        )

        every { globalSettingsProvider.getSettings() } returns expectedSettings

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(expectedSettings, (result as AppResult.Success).data)
    }

    @Test
    fun `invoke - returns settings with null values when provider has no data`() {
        val emptySettings = GlobalSettings(
            privacyPolicyUrl = null,
            termsOfServiceUrl = null,
            contactSupportEmail = null
        )

        every { globalSettingsProvider.getSettings() } returns emptySettings

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(emptySettings, (result as AppResult.Success).data)
    }
}