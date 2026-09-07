package io.github.mudrichenkoevgeny.backend.feature.securityapi.usecase.open.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.OpenPasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.OpenSecuritySettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetOpenSecuritySettingsUseCaseTest {

    private fun sampleSettings(): OpenSecuritySettings = OpenSecuritySettings(
        passwordPolicy = OpenPasswordPolicy(
            minLength = 12,
            requireLetter = true,
            requireUpperCase = true,
            requireLowerCase = true,
            requireDigit = true,
            requireSpecialChar = true
        ),
        otpConfirmation = OtpConfirmation(
            retryAfterSeconds = 60,
            numberOfSymbols = 6,
            expirationSeconds = 300
        )
    )

    @Test
    fun `returns open security settings from provider`() {
        val settings = sampleSettings()
        val securitySettingsProvider = mockk<SecuritySettingsProvider>()

        every { securitySettingsProvider.getOpenSecuritySettings() } returns settings

        val useCase = GetOpenSecuritySettingsUseCase(securitySettingsProvider)
        val result = useCase()

        assertEquals(AppResult.Success(settings), result)
        verify(exactly = 1) { securitySettingsProvider.getOpenSecuritySettings() }
    }
}