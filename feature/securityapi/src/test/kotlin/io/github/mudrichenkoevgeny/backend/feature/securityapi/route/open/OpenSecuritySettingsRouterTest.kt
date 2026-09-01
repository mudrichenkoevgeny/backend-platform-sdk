package io.github.mudrichenkoevgeny.backend.feature.securityapi.route.open

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.securityapi.usecase.open.settings.GetSecuritySettingsUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.network.application.setupOpenTestEnvironment
import io.github.mudrichenkoevgeny.backend.feature.user.network.route.BaseRouterTest
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.SecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.securityapi.network.route.open.security.settings.OpenSecuritySettingsRoutes
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OpenSecuritySettingsRouterTest : BaseRouterTest() {

    private val getSecuritySettingsUseCase = mockk<GetSecuritySettingsUseCase>()

    private val router = OpenSecuritySettingsRouter(
        appLogger = appLogger,
        appErrorParser = appErrorParser,
        getSecuritySettingsUseCase = getSecuritySettingsUseCase
    )

    @BeforeEach
    fun setUp() {
        clearMocks(getSecuritySettingsUseCase)
    }

    private fun sampleSettings() = SecuritySettings(
        recentAuthenticationValiditySeconds = 300,
        recentAuthenticationValiditySecondsForManagement = 60,
        passwordPolicy = PasswordPolicy(
            minLength = 8,
            requireLetter = true,
            requireUpperCase = false,
            requireLowerCase = false,
            requireDigit = false,
            requireSpecialChar = false,
            commonPasswords = emptySet()
        ),
        otpConfirmation = OtpConfirmation(
            retryAfterSeconds = 60,
            numberOfSymbols = 6,
            expirationSeconds = 300
        ),
        mfaTokenExpirationSeconds = 600
    )

    @Test
    fun `get security settings - success`() = testApplication {
        setupOpenTestEnvironment(router)
        val settings = sampleSettings()
        coEvery { getSecuritySettingsUseCase() } returns AppResult.Success(settings)

        val response = client.get(OpenSecuritySettingsRoutes.GET_SECURITY_SETTINGS)

        assertEquals(HttpStatusCode.OK, response.status)
    }
}