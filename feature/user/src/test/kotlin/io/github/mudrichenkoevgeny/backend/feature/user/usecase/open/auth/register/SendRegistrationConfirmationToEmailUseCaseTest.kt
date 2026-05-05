package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.register

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

class SendRegistrationConfirmationToEmailUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val identifierManager = mockk<IdentifierManager>()
    private val otpService = mockk<OtpService>()
    private val emailService = mockk<EmailService>()

    private val useCase = SendRegistrationConfirmationToEmailUseCase(
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
    fun `successfully sends verification code for new email`() = runTest {
        val context = createRequestContext()
        val otpConfirmation = mockk<OtpConfirmation>()
        val otpData = OtpConfirmationData(code = TEST_CODE, otpConfirmation = otpConfirmation)

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
        val context = createRequestContext()
        val otpConfirmation = mockk<OtpConfirmation>()
        val otpData = OtpConfirmationData(code = TEST_CODE, otpConfirmation = otpConfirmation)
        val existingIdentifier = mockk<UserIdentifierInternal>()

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
    fun `returns error when rate limit exceeded`() = runTest {
        val context = createRequestContext()
        val error = UserError.InvalidCredentials()

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Error(error)

        val result = useCase(TEST_EMAIL, context)

        assertTrue(result is AppResult.Error)
        coVerify(exactly = 0) { identifierManager.getUserIdentifierInternalByProvider(any(), any()) }
    }

    @Test
    fun `returns error when otp service fails`() = runTest {
        val context = createRequestContext()
        val error = CommonError.Internal(Throwable())

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