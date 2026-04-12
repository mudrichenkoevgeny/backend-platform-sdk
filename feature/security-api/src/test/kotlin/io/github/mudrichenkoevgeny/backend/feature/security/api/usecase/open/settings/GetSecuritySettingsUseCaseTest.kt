package io.github.mudrichenkoevgeny.backend.feature.security.api.usecase.open.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.SecuritySettings
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class GetSecuritySettingsUseCaseTest {

    @Test
    fun `invoke returns provider result`() {
        val expected = SecuritySettings(
            recentAuthenticationValidityInMinutes = 45L,
            passwordPolicy = PasswordPolicy(minLength = 12, requireDigit = true)
        )
        val provider = FakeProvider(getSettingsResult = AppResult.Success(expected))
        val useCase = GetSecuritySettingsUseCase(provider)

        val result = useCase()

        Assertions.assertTrue(result is AppResult.Success)
        Assertions.assertEquals(expected, (result as AppResult.Success).data)
    }

    private class FakeProvider(
        private val getSettingsResult: AppResult<SecuritySettings>
    ) : SecuritySettingsProvider {
        override suspend fun initialize(): AppResult<Unit> = AppResult.Success(Unit)
        override fun getSettings(): AppResult<SecuritySettings> = getSettingsResult
        override fun getRecentAuthenticationValidityInMinutes(): Long = 0L
        override fun getPasswordPolicy(): PasswordPolicy = PasswordPolicy()
        override suspend fun updateSecuritySettings(securitySettings: SecuritySettings): AppResult<Unit> =
            AppResult.Success(Unit)
    }
}
