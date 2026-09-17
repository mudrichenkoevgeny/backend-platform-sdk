package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.config.model.createTestOpenSecuritySettings
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetOpenSecuritySettingsUseCaseTest {

    @Test
    fun `returns open security settings from provider`() {
        val settings = createTestOpenSecuritySettings()
        val securitySettingsProvider = mockk<SecuritySettingsProvider>()

        every { securitySettingsProvider.getOpenSecuritySettings() } returns settings

        val useCase = GetOpenSecuritySettingsUseCase(securitySettingsProvider)
        val result = useCase()

        assertEquals(AppResult.Success(settings), result)
        verify(exactly = 1) { securitySettingsProvider.getOpenSecuritySettings() }
    }
}
