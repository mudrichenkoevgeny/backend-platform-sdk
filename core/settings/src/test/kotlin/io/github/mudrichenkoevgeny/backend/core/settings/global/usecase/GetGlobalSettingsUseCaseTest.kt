package io.github.mudrichenkoevgeny.backend.core.settings.global.usecase

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.model.GlobalSettings
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetGlobalSettingsUseCaseTest {

    @Test
    fun `execute returns provider result`() {
        val expected = GlobalSettings(
            privacyPolicyUrl = "privacy",
            termsOfServiceUrl = "tos",
            contactSupportEmail = "support@example.com"
        )
        val provider = FakeProvider(
            getSettingsResult = AppResult.Success(expected)
        )
        val useCase = GetGlobalSettingsUseCase(provider)

        val result = useCase.execute()

        assertTrue(result is AppResult.Success)
        assertEquals(expected, (result as AppResult.Success).data)
    }

    private class FakeProvider(
        private val getSettingsResult: AppResult<GlobalSettings>
    ) : GlobalSettingsProvider {
        override suspend fun initialize(): AppResult<Unit> = AppResult.Success(Unit)
        override fun getSettings(): AppResult<GlobalSettings> = getSettingsResult
        override suspend fun updatePrivacyPolicyUrl(url: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateTermsOfServiceUrl(url: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun updateContactSupportEmail(email: String): AppResult<Unit> = AppResult.Success(Unit)
    }
}

