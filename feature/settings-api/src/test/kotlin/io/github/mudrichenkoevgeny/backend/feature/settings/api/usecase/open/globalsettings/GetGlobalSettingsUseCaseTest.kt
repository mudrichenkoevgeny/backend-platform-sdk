package io.github.mudrichenkoevgeny.backend.feature.settings.api.usecase.open.globalsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.GlobalSettings
import org.junit.jupiter.api.Assertions
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

        val result = useCase()

        Assertions.assertTrue(result is AppResult.Success)
        Assertions.assertEquals(expected, (result as AppResult.Success).data)
    }

    private class FakeProvider(
        private val getSettingsResult: AppResult<GlobalSettings>
    ) : GlobalSettingsProvider {
        override suspend fun initialize(): AppResult<Unit> = AppResult.Success(Unit)
        override fun getSettings(): AppResult<GlobalSettings> = getSettingsResult
        override suspend fun updateGlobalSettings(globalSettings: GlobalSettings): AppResult<Unit> =
            AppResult.Success(Unit)
    }
}