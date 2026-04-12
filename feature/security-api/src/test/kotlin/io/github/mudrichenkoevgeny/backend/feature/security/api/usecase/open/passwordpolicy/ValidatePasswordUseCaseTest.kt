package io.github.mudrichenkoevgeny.backend.feature.security.api.usecase.open.passwordpolicy

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.error.naming.SecurityErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.validator.PasswordPolicyValidatorImpl
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ValidatePasswordUseCaseTest {

    @Test
    fun `invoke returns success when password satisfies policy`() {
        val provider = mockk<SecuritySettingsProvider>()
        val policy = PasswordPolicy(minLength = 8, requireDigit = true)
        every { provider.getPasswordPolicy() } returns policy

        val useCase = ValidatePasswordUseCase(
            securitySettingsProvider = provider,
            passwordPolicyValidator = PasswordPolicyValidatorImpl()
        )

        val result = useCase("GoodPass123")

        assertEquals(AppResult.Success(Unit), result)
    }

    @Test
    fun `invoke returns PasswordTooWeak with detailed publicArgs when password violates policy`() {
        val provider = mockk<SecuritySettingsProvider>()
        val policy = PasswordPolicy(minLength = 12, requireDigit = true, requireUpperCase = true)
        every { provider.getPasswordPolicy() } returns policy

        val useCase = ValidatePasswordUseCase(
            securitySettingsProvider = provider,
            passwordPolicyValidator = PasswordPolicyValidatorImpl()
        )

        val result = useCase("weakpass")

        val error = (result as AppResult.Error).error as SecurityError.PasswordTooWeak
        val publicArgs = error.publicArgs!!

        assertEquals(12, publicArgs[SecurityErrorArgs.PASSWORD_MIN_LENGTH])
        assertTrue(publicArgs[SecurityErrorArgs.PASSWORD_FAIL_TOO_SHORT] is Boolean)
        assertTrue(publicArgs[SecurityErrorArgs.PASSWORD_FAIL_NO_DIGIT] is Boolean)
        assertTrue(publicArgs[SecurityErrorArgs.PASSWORD_FAIL_NO_UPPERCASE] is Boolean)
    }
}