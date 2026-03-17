package io.github.mudrichenkoevgeny.backend.feature.user.usecase.auth.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthSettings
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AvailableAuthProviders
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetAuthSettingsUseCaseTest {

    private val authSettingsProvider = mockk<AuthSettingsProvider>()
    private val useCase = GetAuthSettingsUseCase(authSettingsProvider = authSettingsProvider)

    @Test
    fun `execute returns result from auth settings provider`() {
        val settings = AuthSettings(
            availableAuthProviders = AvailableAuthProviders(
                primary = listOf(UserAuthProvider.EMAIL),
                secondary = listOf(UserAuthProvider.GOOGLE)
            )
        )
        every { authSettingsProvider.getSettings() } returns AppResult.Success(settings)

        val result = useCase.execute()

        assertTrue(result is AppResult.Success)
        assertEquals(settings, (result as AppResult.Success).data)
    }
}
