package io.github.mudrichenkoevgeny.backend.core.settings.global.usecase

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.model.GlobalSettings
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SeedGlobalSettingsUseCaseTest {

    @Test
    fun `execute delegates to provider initialize`() = runBlocking {
        val provider = RecordingProvider()
        val useCase = SeedGlobalSettingsUseCase(provider)

        val result = useCase.execute()

        assertTrue(result is AppResult.Success)
        assertTrue(provider.initializeCalled)
    }

    private class RecordingProvider : GlobalSettingsProvider {
        var initializeCalled: Boolean = false

        override suspend fun initialize(): AppResult<Unit> {
            initializeCalled = true
            return AppResult.Success(Unit)
        }

        override fun getSettings(): AppResult<GlobalSettings> {
            error("Not used")
        }

        override suspend fun updatePrivacyPolicyUrl(url: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateTermsOfServiceUrl(url: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateContactSupportEmail(email: String): AppResult<Unit> = AppResult.Success(Unit)
    }
}

