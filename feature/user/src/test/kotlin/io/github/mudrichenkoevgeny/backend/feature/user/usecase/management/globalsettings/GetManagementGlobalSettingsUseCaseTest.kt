package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.globalsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.domain.model.globalsettings.createTestManagementGlobalSettings
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
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
        val expectedSettings = createTestManagementGlobalSettings()

        every { globalSettingsProvider.getManagementGlobalSettings() } returns expectedSettings

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(expectedSettings, (result as AppResult.Success).data)
    }
}
