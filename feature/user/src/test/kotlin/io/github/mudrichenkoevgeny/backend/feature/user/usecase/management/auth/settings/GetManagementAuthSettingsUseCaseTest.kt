package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auth.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetManagementAuthSettingsUseCaseTest {

    private val authSettingsProvider = mockk<AuthSettingsProvider>()
    private val useCase = GetManagementAuthSettingsUseCase(authSettingsProvider)

    @Test
    fun `invoke - returns management settings from provider`() {
        val expectedSettings = mockk<ManagementAuthSettings>()

        every { authSettingsProvider.getManagementAuthSettings() } returns expectedSettings

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertEquals(expectedSettings, (result as AppResult.Success).data)
    }
}