package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.config.model.createTestManagementSecuritySettings
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetManagementSecuritySettingsUseCaseTest {

    @Test
    fun `returns management security settings from provider`() {
        val settings = createTestManagementSecuritySettings()
        val securitySettingsProvider = mockk<SecuritySettingsProvider>()

        every { securitySettingsProvider.getManagementSecuritySettings() } returns settings

        val useCase = GetManagementSecuritySettingsUseCase(securitySettingsProvider)
        val result = useCase()

        assertEquals(AppResult.Success(settings), result)
        verify(exactly = 1) { securitySettingsProvider.getManagementSecuritySettings() }
    }
}
