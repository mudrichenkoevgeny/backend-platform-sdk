package io.github.mudrichenkoevgeny.backend.feature.user.usecase.configuration

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthSettings
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AvailableAuthProviders
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.backend.core.settings.global.model.GlobalSettings
import io.github.mudrichenkoevgeny.backend.core.security.settings.model.SecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.model.PasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetUserConfigurationUseCaseTest {

    private val globalSettingsProvider = mockk<GlobalSettingsProvider>()
    private val securitySettingsProvider = mockk<SecuritySettingsProvider>()
    private val authSettingsProvider = mockk<AuthSettingsProvider>()

    private val useCase = GetUserConfigurationUseCase(
        globalSettingsProvider = globalSettingsProvider,
        securitySettingsProvider = securitySettingsProvider,
        authSettingsProvider = authSettingsProvider
    )

    @Test
    fun `execute returns combined configuration when all providers succeed`() {
        val global = GlobalSettings(
            privacyPolicyUrl = "https://example.com/privacy",
            termsOfServiceUrl = null,
            contactSupportEmail = "support@example.com"
        )
        val security = SecuritySettings(
            passwordPolicy = PasswordPolicy(minLength = 8, requireDigit = true)
        )
        val auth = AuthSettings(
            availableAuthProviders = AvailableAuthProviders(
                primary = listOf(UserAuthProvider.EMAIL),
                secondary = listOf(UserAuthProvider.GOOGLE)
            )
        )

        every { globalSettingsProvider.getSettings() } returns AppResult.Success(global)
        every { securitySettingsProvider.getSettings() } returns AppResult.Success(security)
        every { authSettingsProvider.getSettings() } returns AppResult.Success(auth)

        val result = useCase.execute()

        assertTrue(result is AppResult.Success)
        val config = (result as AppResult.Success).data
        assertEquals(global, config.globalSettings)
        assertEquals(security, config.securitySettings)
        assertEquals(auth, config.authSettings)
    }

    @Test
    fun `execute returns first error when global provider fails`() {
        val error = UserError.InvalidAccessToken()
        every { globalSettingsProvider.getSettings() } returns AppResult.Error(error)
        every { securitySettingsProvider.getSettings() } returns AppResult.Success(
            SecuritySettings(PasswordPolicy(minLength = 8, requireDigit = true))
        )
        every { authSettingsProvider.getSettings() } returns AppResult.Success(
            AuthSettings(AvailableAuthProviders(primary = emptyList(), secondary = emptyList()))
        )

        val result = useCase.execute()

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)
    }

    @Test
    fun `execute returns first error when security provider fails`() {
        val global = GlobalSettings(null, null, null)
        val error = UserError.InvalidAccessToken()
        every { globalSettingsProvider.getSettings() } returns AppResult.Success(global)
        every { securitySettingsProvider.getSettings() } returns AppResult.Error(error)
        every { authSettingsProvider.getSettings() } returns AppResult.Success(
            AuthSettings(AvailableAuthProviders(primary = emptyList(), secondary = emptyList()))
        )

        val result = useCase.execute()

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)
    }

    @Test
    fun `execute returns first error when auth provider fails`() {
        val global = GlobalSettings(null, null, null)
        val security = SecuritySettings(PasswordPolicy(minLength = 8, requireDigit = true))
        val error = UserError.InvalidAccessToken()
        every { globalSettingsProvider.getSettings() } returns AppResult.Success(global)
        every { securitySettingsProvider.getSettings() } returns AppResult.Success(security)
        every { authSettingsProvider.getSettings() } returns AppResult.Error(error)

        val result = useCase.execute()

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)
    }
}
