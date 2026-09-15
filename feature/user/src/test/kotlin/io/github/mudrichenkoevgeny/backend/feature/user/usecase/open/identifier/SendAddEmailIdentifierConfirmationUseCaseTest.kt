package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpConfirmationData
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.emailrestriction.createTestEmailRestrictionPolicy
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SendAddEmailIdentifierConfirmationUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val authSettingsProvider = mockk<AuthSettingsProvider>()
    private val emailRestrictionPolicyValidator = EmailRestrictionPolicyValidator()
    private val identifierManager = mockk<IdentifierManager>()
    private val otpService = mockk<OtpService>()
    private val emailService = mockk<EmailService>()

    private val useCase = SendAddEmailIdentifierConfirmationUseCase(
        rateLimiter = rateLimiter,
        authSettingsProvider = authSettingsProvider,
        emailRestrictionPolicyValidator = emailRestrictionPolicyValidator,
        identifierManager = identifierManager,
        otpService = otpService,
        emailService = emailService
    )

    private val context = createTestAuthenticatedRequestContext()
    private val clientInfo = context.clientInfo

    @BeforeEach
    fun setUp() {
        every { authSettingsProvider.getOpenEmailRestrictionPolicy() } returns createTestEmailRestrictionPolicy()
    }

    @Test
    fun `successfully sends verification code when email is new`() = runTest {
        val otpConfirmation = mockk<OtpConfirmation>()
        val otpData = OtpConfirmationData(
            otpConfirmation = otpConfirmation,
            code = TEST_CODE
        )

        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.SEND_OTP_EMAIL, TEST_EMAIL)
        } returns AppResult.Success(Unit)

        coEvery {
            identifierManager.getUserIdentifierInternalByProvider(UserAuthProvider.EMAIL, TEST_EMAIL)
        } returns AppResult.Success(null)

        coEvery {
            otpService.getOtp(TEST_EMAIL, UserOtpVerificationType.EMAIL_VERIFICATION)
        } returns AppResult.Success(otpData)

        coEvery {
            emailService.sendVerificationCode(TEST_EMAIL, TEST_CODE, clientInfo.deviceInfo.language)
        } returns AppResult.Success(Unit)

        val result = useCase(TEST_EMAIL, context)

        assertEquals(AppResult.Success(otpConfirmation), result)

        coVerify(exactly = 1) {
            emailService.sendVerificationCode(any(), any(), any())
        }

        coVerify(exactly = 0) {
            emailService.sendAlreadyRegisteredEmail(any(), any(), any(), any())
        }
    }

    @Test
    fun `successfully sends notification when email is already registered`() = runTest {
        val identifier = mockk<UserIdentifierInternal>()
        val otpConfirmation = mockk<OtpConfirmation>()
        val otpData = OtpConfirmationData(
            otpConfirmation = otpConfirmation,
            code = TEST_CODE
        )

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)

        coEvery {
            identifierManager.getUserIdentifierInternalByProvider(UserAuthProvider.EMAIL, TEST_EMAIL)
        } returns AppResult.Success(identifier)

        coEvery { otpService.getOtp(any(), any()) } returns AppResult.Success(otpData)

        coEvery {
            emailService.sendAlreadyRegisteredEmail(
                email = TEST_EMAIL,
                ipAddress = clientInfo.ipAddress,
                deviceName = clientInfo.deviceInfo.deviceName,
                language = clientInfo.deviceInfo.language
            )
        } returns AppResult.Success(Unit)

        val result = useCase(TEST_EMAIL, context)

        assertEquals(AppResult.Success(otpConfirmation), result)

        coVerify(exactly = 1) {
            emailService.sendAlreadyRegisteredEmail(any(), any(), any(), any())
        }

        coVerify(exactly = 0) {
            emailService.sendVerificationCode(any(), any(), any())
        }
    }

    @Test
    fun `returns error when rate limit exceeded`() = runTest {
        val error = mockk<AppError>()

        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.SEND_OTP_EMAIL, TEST_EMAIL)
        } returns AppResult.Error(error)

        val result = useCase(TEST_EMAIL, context)

        assertEquals(AppResult.Error(error), result)

        coVerify(exactly = 0) { otpService.getOtp(any(), any()) }
    }

    @Test
    fun `returns error when email is not allowed by restriction policy`() = runTest {
        coEvery {
            rateLimiter.checkRateLimit(any(), any())
        } returns AppResult.Success(Unit)

        every { authSettingsProvider.getOpenEmailRestrictionPolicy() } returns createTestEmailRestrictionPolicy(
            isBlacklistEnabled = true,
            blacklist = listOf("@tempmail.com")
        )

        val result = useCase("user@tempmail.com", context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.EmailNotAllowed)
    }

    companion object {
        private const val TEST_EMAIL = "new@example.com"
        private const val TEST_CODE = "123456"
    }
}