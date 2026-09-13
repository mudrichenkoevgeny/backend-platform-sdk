package io.github.mudrichenkoevgeny.backend.core.settings.usecase.system.globalsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.RecordingGlobalSettingsProvider
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SeedGlobalSettingsUseCaseTest {

    @Test
    fun `execute delegates to provider initialize`() = runBlocking {
        val provider = RecordingGlobalSettingsProvider()
        val useCase = SeedGlobalSettingsUseCase(provider)

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertTrue(provider.initializeCalled)
    }
}
