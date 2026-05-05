package io.github.mudrichenkoevgeny.backend.core.settings.usecase.system.globalsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.GlobalSettings
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SeedGlobalSettingsUseCaseTest {

    @Test
    fun `execute delegates to provider initialize`() = runBlocking {
        val provider = RecordingProvider()
        val useCase = SeedGlobalSettingsUseCase(provider)

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertTrue(provider.initializeCalled)
    }

    private class RecordingProvider : GlobalSettingsProvider {
        var initializeCalled: Boolean = false

        override suspend fun initialize(): AppResult<Unit> {
            initializeCalled = true
            return AppResult.Success(Unit)
        }

        override fun getSettings(): GlobalSettings {
            error("Not used")
        }

        override fun getPrivacyPolicyUrl(): String? = null
        override fun getTermsOfServiceUrl(): String? = null
        override fun getContactSupportEmail(): String? = null

        override suspend fun updateGlobalSettings(globalSettings: GlobalSettings): AppResult<Unit> =
            AppResult.Success(Unit)
    }
}