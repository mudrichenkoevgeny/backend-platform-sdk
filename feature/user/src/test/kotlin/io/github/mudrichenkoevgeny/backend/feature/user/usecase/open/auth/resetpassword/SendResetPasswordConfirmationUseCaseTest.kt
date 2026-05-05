package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.resetpassword

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpConfirmationData
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.EmailService
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.UserOtpVerificationType
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SendResetPasswordConfirmationUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val identifierManager = mockk<IdentifierManager>()
    private val otpService = mockk<OtpService>()
    private val emailService = mockk<EmailService>()

    private val useCase = SendResetPasswordConfirmationUseCase(
        rateLimiter = rateLimiter,
        identifierManager = identifierManager,
        otpService = otpService,
        emailService = emailService
    )

    private fun createRequestContext() = RequestContext(
        traceId = null,
        userId = null,
        userRole = null,
        sessionId = null,
        clientInfo = ClientInfo()
    )

    @Test
    fun `successfully sends real email when user exists`() = runTest {
        val context = createRequestContext()
        val otpConfirmation = mockk<OtpConfirmation>()
        val otpData = OtpConfirmationData(
            code = TEST_CODE,
            otpConfirmation = otpConfirmation
        )
        val identifier = mockk<UserIdentifierInternal>()

        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.SEND_OTP_EMAIL, TEST_EMAIL)
        } returns AppResult.Success(Unit)
        coEvery {
            identifierManager.getUserIdentifierInternalByProvider(UserAuthProvider.EMAIL, TEST_EMAIL)
        } returns AppResult.Success(identifier)
        coEvery {
            otpService.getOtp(TEST_EMAIL, UserOtpVerificationType.EMAIL_PASSWORD_RESET)
        } returns AppResult.Success(otpData)
        coEvery {
            emailService.sendResetPasswordVerificationCode(any(), any(), any())
        } returns AppResult.Success(Unit)

        val result = useCase(TEST_EMAIL, context)

        assertEquals(AppResult.Success(otpConfirmation), result)
        coVerify(exactly = 1) {
            emailService.sendResetPasswordVerificationCode(TEST_EMAIL, TEST_CODE, any())
        }
        coVerify(exactly = 0) { emailService.fakeSendEmail() }
    }

    @Test
    fun `successfully sends fake email when user does not exist`() = runTest {
        val context = createRequestContext()
        val otpConfirmation = mockk<OtpConfirmation>()
        val otpData = OtpConfirmationData(
            code = TEST_CODE,
            otpConfirmation = otpConfirmation
        )

        coEvery {
            rateLimiter.checkRateLimit(any(), any())
        } returns AppResult.Success(Unit)
        coEvery {
            identifierManager.getUserIdentifierInternalByProvider(any(), any())
        } returns AppResult.Success(null)
        coEvery {
            otpService.getOtp(any(), any())
        } returns AppResult.Success(otpData)
        coEvery { emailService.fakeSendEmail() } returns AppResult.Success(Unit)

        val result = useCase(TEST_EMAIL, context)

        assertEquals(AppResult.Success(otpConfirmation), result)
        coVerify(exactly = 1) { emailService.fakeSendEmail() }
        coVerify(exactly = 0) { emailService.sendResetPasswordVerificationCode(any(), any(), any()) }
    }

    @Test
    fun `returns error when rate limit exceeded`() = runTest {
        val context = createRequestContext()
        val error = UserError.InvalidCredentials()

        coEvery {
            rateLimiter.checkRateLimit(any(), any())
        } returns AppResult.Error(error)

        val result = useCase(TEST_EMAIL, context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)
        coVerify(exactly = 0) { identifierManager.getUserIdentifierInternalByProvider(any(), any()) }
    }

    @Test
    fun `returns error when email service fails on real send`() = runTest {
        val context = createRequestContext()
        val otpData = OtpConfirmationData(
            code = TEST_CODE,
            otpConfirmation = mockk()
        )
        val error = CommonError.Internal(Throwable())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery {
            identifierManager.getUserIdentifierInternalByProvider(any(), any())
        } returns AppResult.Success(mockk())
        coEvery { otpService.getOtp(any(), any()) } returns AppResult.Success(otpData)
        coEvery {
            emailService.sendResetPasswordVerificationCode(any(), any(), any())
        } returns AppResult.Error(error)

        val result = useCase(TEST_EMAIL, context)

        assertEquals(AppResult.Error(error), result)
    }

    companion object {
        private const val TEST_EMAIL = "reset@example.com"
        private const val TEST_CODE = "123456"
    }
}