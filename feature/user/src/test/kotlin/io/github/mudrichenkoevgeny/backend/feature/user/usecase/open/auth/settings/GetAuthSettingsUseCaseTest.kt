package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.PublicAuthSettings
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetAuthSettingsUseCaseTest {

    private val authSettingsProvider = mockk<AuthSettingsProvider>()

    private val useCase = GetAuthSettingsUseCase(
        authSettingsProvider = authSettingsProvider
    )

    @Test
    fun `successfully returns public auth settings`() {
        val publicAuthSettings = mockk<PublicAuthSettings>()

        every {
            authSettingsProvider.getPublicAuthSettings()
        } returns publicAuthSettings

        val result = useCase()

        assertEquals(AppResult.Success(publicAuthSettings), result)
    }
}