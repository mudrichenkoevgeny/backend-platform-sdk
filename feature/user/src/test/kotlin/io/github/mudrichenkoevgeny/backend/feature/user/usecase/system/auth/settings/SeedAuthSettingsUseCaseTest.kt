package io.github.mudrichenkoevgeny.backend.feature.user.usecase.system.auth.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.AvailableAuthProviders
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.PublicAuthSettings
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SeedAuthSettingsUseCaseTest {

    @Test
    fun `invoke delegates to provider initialize`() = runBlocking {
        val provider = RecordingProvider()
        val useCase = SeedAuthSettingsUseCase(provider)

        val result = useCase()

        assertTrue(result is AppResult.Success)
        assertTrue(provider.initializeCalled)
    }

    private class RecordingProvider : AuthSettingsProvider {
        var initializeCalled: Boolean = false

        override suspend fun initialize(): AppResult<Unit> {
            initializeCalled = true
            return AppResult.Success(Unit)
        }

        override fun getManagementAuthSettings(): ManagementAuthSettings = error("Not used")
        override fun getPublicAuthSettings(): PublicAuthSettings = error("Not used")
        override fun getAvailableAuthProviders(): AvailableAuthProviders = error("Not used")
        override fun getMaxTotalIdentifiers(): Int = 0
        override fun getMaxEmailIdentifiers(): Int = 0
        override fun getMaxPhoneIdentifiers(): Int = 0
        override fun getMaxIdentifiersPerExternalProvider(): Int = 0
        override fun getMaxActiveSessions(): Int = 0
        override fun getAccessTokenExpirationSeconds(): Int = 0
        override fun getRefreshTokenExpirationSeconds(): Int = 0
        override fun getAccountDeletionDelaySeconds(): Int = 0

        override suspend fun updateManagementAuthSettings(
            managementAuthSettings: ManagementAuthSettings
        ): AppResult<Unit> = AppResult.Success(Unit)
    }
}