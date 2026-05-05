package io.github.mudrichenkoevgeny.backend.core.security.usecase.open.passwordpolicy

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicyValidatorResult
import io.github.mudrichenkoevgeny.shared.foundation.core.security.error.naming.SecurityErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.validator.PasswordPolicyValidator
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.validator.PasswordPolicyValidatorImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ValidatePasswordUseCaseTest {

    private val securitySettingsProvider = mockk<SecuritySettingsProvider>()
    private val passwordPolicyValidator = PasswordPolicyValidatorImpl()
    private lateinit var useCase: ValidatePasswordUseCase

    private val defaultPolicy = PasswordPolicy(
        minLength = 8,
        requireDigit = true,
        requireUpperCase = true,
        requireLowerCase = true,
        requireSpecialChar = false
    )

    @BeforeEach
    fun setup() {
        useCase = ValidatePasswordUseCase(
            securitySettingsProvider = securitySettingsProvider,
            passwordPolicyValidator = passwordPolicyValidator
        )
    }

    @Test
    fun `invoke returns success when password satisfies all policy rules`() {
        every { securitySettingsProvider.getPasswordPolicy() } returns defaultPolicy

        val result = useCase("StrongPass123")

        assertEquals(AppResult.Success(Unit), result)
        verify(exactly = 1) { securitySettingsProvider.getPasswordPolicy() }
    }

    @Test
    fun `invoke returns error when password is too short`() {
        val policy = defaultPolicy.copy(minLength = 10)
        every { securitySettingsProvider.getPasswordPolicy() } returns policy

        val result = useCase("Short1")

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error as SecurityError.PasswordTooWeak

        val args = error.publicArgs!!
        assertEquals(true, args[SecurityErrorArgs.PASSWORD_FAIL_TOO_SHORT])
        assertEquals(10, args[SecurityErrorArgs.PASSWORD_MIN_LENGTH])
    }

    @Test
    fun `invoke returns error when password misses required character types`() {
        val policy = defaultPolicy.copy(requireUpperCase = true, requireDigit = true)
        every { securitySettingsProvider.getPasswordPolicy() } returns policy

        val result = useCase("onlylower")

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error as SecurityError.PasswordTooWeak

        val args = error.publicArgs!!
        assertEquals(true, args[SecurityErrorArgs.PASSWORD_FAIL_NO_UPPERCASE])
        assertEquals(true, args[SecurityErrorArgs.PASSWORD_FAIL_NO_DIGIT])
        assertTrue(args[SecurityErrorArgs.PASSWORD_FAIL_NO_LOWERCASE] != true)
    }

    @Test
    fun `invoke handles validator failures correctly`() {
        val mockedValidator = mockk<PasswordPolicyValidator>()
        val customUseCase = ValidatePasswordUseCase(securitySettingsProvider, mockedValidator)

        every { securitySettingsProvider.getPasswordPolicy() } returns defaultPolicy
        every { mockedValidator.validate(any(), any()) } returns
                PasswordPolicyValidatorResult.Fail(
                    reasons = emptyList(),
                    passwordPolicy = defaultPolicy
                )

        val result = customUseCase("any")

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is SecurityError.PasswordTooWeak)
    }
}