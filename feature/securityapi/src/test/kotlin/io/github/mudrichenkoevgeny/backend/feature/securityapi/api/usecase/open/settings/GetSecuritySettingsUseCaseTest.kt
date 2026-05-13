package io.github.mudrichenkoevgeny.backend.feature.securityapi.api.usecase.open.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.SecuritySettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetSecuritySettingsUseCaseTest {

    private fun sampleSettings(): SecuritySettings = SecuritySettings(
        recentAuthenticationValiditySeconds = 300,
        recentAuthenticationValiditySecondsForManagement = 60,
        passwordPolicy = PasswordPolicy(
            minLength = 12,
            requireLetter = true,
            requireUpperCase = true,
            requireLowerCase = true,
            requireDigit = true,
            requireSpecialChar = true,
            commonPasswords = setOf("12345678", "password")
        ),
        otpConfirmation = OtpConfirmation(
            retryAfterSeconds = 60,
            numberOfSymbols = 6,
            expirationSeconds = 300
        ),
        mfaTokenExpirationSeconds = 600
    )

    @Test
    fun `returns security settings from provider`() {
        val settings = sampleSettings()
        val securitySettingsProvider = mockk<SecuritySettingsProvider>()

        every { securitySettingsProvider.getSettings() } returns settings

        val useCase = GetSecuritySettingsUseCase(securitySettingsProvider)
        val result = useCase()

        assertEquals(AppResult.Success(settings), result)
        verify(exactly = 1) { securitySettingsProvider.getSettings() }
    }
}