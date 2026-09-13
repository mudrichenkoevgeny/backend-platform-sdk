package io.github.mudrichenkoevgeny.backend.feature.securityapi.usecase.management.settings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.ManagementPasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.ManagementSecuritySettings
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetManagementSecuritySettingsUseCaseTest {

    private fun sampleSettings(): ManagementSecuritySettings = ManagementSecuritySettings(
        recentAuthenticationValiditySecondsForOpenUser = 300,
        recentAuthenticationValiditySecondsForManagementUser = 60,
        passwordPolicy = ManagementPasswordPolicy(
            minLength = 12,
            requireLetter = true,
            requireUpperCase = true,
            requireLowerCase = true,
            requireDigit = true,
            requireSpecialChar = true,
            commonPasswords = emptySet()
        ),
        otpConfirmation = OtpConfirmation(
            retryAfterSeconds = 60,
            numberOfSymbols = 6,
            expirationSeconds = 300
        ),
        accountLockoutPolicy = SecurityConfig.DEFAULT_ACCOUNT_LOCKOUT_POLICY,
        openIpRestrictionPolicy = SecurityConfig.DEFAULT_IP_RESTRICTION_POLICY,
        managementIpRestrictionPolicy = SecurityConfig.DEFAULT_IP_RESTRICTION_POLICY,
        mfaTokenExpirationSeconds = 600,
        maxRequestsPerPeriod = 100,
        rateLimitPeriodSeconds = 60
    )

    @Test
    fun `returns management security settings from provider`() {
        val settings = sampleSettings()
        val securitySettingsProvider = mockk<SecuritySettingsProvider>()

        every { securitySettingsProvider.getManagementSecuritySettings() } returns settings

        val useCase = GetManagementSecuritySettingsUseCase(securitySettingsProvider)
        val result = useCase()

        assertEquals(AppResult.Success(settings), result)
        verify(exactly = 1) { securitySettingsProvider.getManagementSecuritySettings() }
    }
}