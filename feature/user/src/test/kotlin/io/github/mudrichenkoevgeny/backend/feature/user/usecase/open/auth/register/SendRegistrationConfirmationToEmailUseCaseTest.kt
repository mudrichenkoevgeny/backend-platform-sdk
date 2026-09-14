package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.register

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpConfirmationData
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.emailrestriction.createTestEmailRestrictionPolicy
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.EmailService
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.UserOtpVerificationType
import io.github.mudrichenkoevgeny.backend.feature.user.validator.emailrestriction.EmailRestrictionPolicyValidator
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SendRegistrationConfirmationToEmailUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val authSettingsProvider = mockk<AuthSettingsProvider>()
    private val emailRestrictionPolicyValidator = EmailRestrictionPolicyValidator()
    private val identifierManager = mockk<IdentifierManager>()
    private val otpService = mockk<OtpService>()
    private val emailService = mockk<EmailService>()

    private val useCase = SendRegistrationConfirmationToEmailUseCase(
        rateLimiter = rateLimiter,
        authSettingsProvider = authSettingsProvider,
        emailRestrictionPolicyValidator = emailRestrictionPolicyValidator,
        identifierManager = identifierManager,
        otpService = otpService,
        emailService = emailService
    )

    @Test
    fun `successfully sends verification code for new email`() = runTest {
        val context = createTestRequestContext()
        val otpConfirmation = mockk<OtpConfirmation>()
        val otpData = OtpConfirmationData(code = TEST_CODE, otpConfirmation = otpConfirmation)

        every { authSettingsProvider.getIsRegistrationEnabled() } returns true
        every { authSettingsProvider.getOpenEmailRestrictionPolicy() } returns createTestEmailRestrictionPolicy()
        coEvery { rateLimiter.checkRateLimit(UserRateLimitAction.SEND_OTP_EMAIL, TEST_EMAIL) } returns AppResult.Success(Unit)
        coEvery { identifierManager.getUserIdentifierInternalByProvider(UserAuthProvider.EMAIL, TEST_EMAIL) } returns AppResult.Success(null)
        coEvery { otpService.getOtp(TEST_EMAIL, UserOtpVerificationType.EMAIL_VERIFICATION) } returns AppResult.Success(otpData)
        coEvery { emailService.sendVerificationCode(any(), any(), any()) } returns AppResult.Success(Unit)

        val result = useCase(TEST_EMAIL, context)

        assertEquals(AppResult.Success(otpConfirmation), result)
        coVerify(exactly = 1) { emailService.sendVerificationCode(TEST_EMAIL, TEST_CODE, any()) }
    }

    @Test
    fun `sends security notification when email is already registered`() = runTest {
        val context = createTestRequestContext()
        val otpConfirmation = mockk<OtpConfirmation>()
        val otpData = OtpConfirmationData(code = TEST_CODE, otpConfirmation = otpConfirmation)
        val existingIdentifier = mockk<UserIdentifierInternal>()

        every { authSettingsProvider.getIsRegistrationEnabled() } returns true
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { identifierManager.getUserIdentifierInternalByProvider(any(), any()) } returns AppResult.Success(existingIdentifier)
        coEvery { otpService.getOtp(any(), any()) } returns AppResult.Success(otpData)
        coEvery { emailService.sendAlreadyRegisteredEmail(any(), any(), any(), any()) } returns AppResult.Success(Unit)

        val result = useCase(TEST_EMAIL, context)

        assertEquals(AppResult.Success(otpConfirmation), result)
        coVerify(exactly = 1) { emailService.sendAlreadyRegisteredEmail(TEST_EMAIL, any(), any(), any()) }
        coVerify(exactly = 0) { emailService.sendVerificationCode(any(), any(), any()) }
    }

    @Test
    fun `returns error when registration is disabled`() = runTest {
        val context = createTestRequestContext()

        every { authSettingsProvider.getIsRegistrationEnabled() } returns false

        val result = useCase(TEST_EMAIL, context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.RegistrationDisabled)
        coVerify(exactly = 0) { rateLimiter.checkRateLimit(any(), any()) }
    }

    @Test
    fun `returns error when email is not allowed by restriction policy`() = runTest {
        val context = createTestRequestContext()

        every { authSettingsProvider.getIsRegistrationEnabled() } returns true
        every { authSettingsProvider.getOpenEmailRestrictionPolicy() } returns createTestEmailRestrictionPolicy(
            isBlacklistEnabled = true,
            blacklist = listOf("@tempmail.com")
        )

        val result = useCase("user@tempmail.com", context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.EmailNotAllowed)
        coVerify(exactly = 0) { rateLimiter.checkRateLimit(any(), any()) }
    }

    @Test
    fun `returns error when rate limit exceeded`() = runTest {
        val context = createTestRequestContext()
        val error = UserError.InvalidCredentials()

        every { authSettingsProvider.getIsRegistrationEnabled() } returns true
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Error(error)

        val result = useCase(TEST_EMAIL, context)

        assertTrue(result is AppResult.Error)
        coVerify(exactly = 0) { identifierManager.getUserIdentifierInternalByProvider(any(), any()) }
    }

    @Test
    fun `returns error when otp service fails`() = runTest {
        val context = createTestRequestContext()
        val error = CommonError.Internal(Throwable())

        every { authSettingsProvider.getIsRegistrationEnabled() } returns true
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { identifierManager.getUserIdentifierInternalByProvider(any(), any()) } returns AppResult.Success(null)
        coEvery { otpService.getOtp(any(), any()) } returns AppResult.Error(error)

        val result = useCase(TEST_EMAIL, context)

        assertEquals(AppResult.Error(error), result)
    }

    companion object {
        private const val TEST_EMAIL = "test@example.com"
        private const val TEST_CODE = "123456"
    }
}
