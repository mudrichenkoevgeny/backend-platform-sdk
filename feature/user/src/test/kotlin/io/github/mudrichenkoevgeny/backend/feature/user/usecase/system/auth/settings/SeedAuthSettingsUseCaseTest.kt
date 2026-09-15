package io.github.mudrichenkoevgeny.backend.feature.user.usecase.system.auth.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.TestAuthSettingsProvider
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SeedAuthSettingsUseCaseTest {

    @Test
    fun `invoke delegates to provider initialize`() = runBlocking {
        val provider = TestAuthSettingsProvider()
        val useCase = SeedAuthSettingsUseCase(provider)

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertTrue(provider.initializeCalled)
    }
}
