package io.github.mudrichenkoevgeny.backend.core.settings.usecase.system.globalsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.ManagementGlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.OpenGlobalSettings
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

        override fun getOpenGlobalSettings(): OpenGlobalSettings {
            error("Not used")
        }

        override fun getManagementGlobalSettings(): ManagementGlobalSettings {
            error("Not used")
        }

        override fun getPrivacyPolicyUrl(): String? = null
        override fun getTermsOfServiceUrl(): String? = null
        override fun getContactSupportEmail(): String? = null
        override fun getMinSupportedAppVersions(): Map<ClientType, String> = emptyMap()
        override fun getIsTracingEnabled(): Boolean = false
        override fun getIsMetricsEnabled(): Boolean = false
        override fun getIsVerboseLoggingEnabled(): Boolean = false

        override suspend fun updateManagementGlobalSettings(managementGlobalSettings: ManagementGlobalSettings): AppResult<Unit> =
            AppResult.Success(Unit)
    }
}