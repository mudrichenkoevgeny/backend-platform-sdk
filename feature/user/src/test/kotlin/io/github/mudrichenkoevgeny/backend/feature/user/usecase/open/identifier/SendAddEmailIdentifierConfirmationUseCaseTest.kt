package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpConfirmationData
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.EmailService
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.UserOtpVerificationType
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SendAddEmailIdentifierConfirmationUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val identifierManager = mockk<IdentifierManager>()
    private val otpService = mockk<OtpService>()
    private val emailService = mockk<EmailService>()

    private val useCase = SendAddEmailIdentifierConfirmationUseCase(
        rateLimiter = rateLimiter,
        identifierManager = identifierManager,
        otpService = otpService,
        emailService = emailService
    )

    private val clientInfo = ClientInfo()

    private val context = AuthenticatedRequestContext(
        traceId = null,
        userId = UserId.generate(),
        userRole = UserRole.USER,
        sessionId = UserSessionId.generate(),
        clientInfo = clientInfo
    )

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
        val error = mockk<io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError>()

        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.SEND_OTP_EMAIL, TEST_EMAIL)
        } returns AppResult.Error(error)

        val result = useCase(TEST_EMAIL, context)

        assertEquals(AppResult.Error(error), result)

        coVerify(exactly = 0) { otpService.getOtp(any(), any()) }
    }

    companion object {
        private const val TEST_EMAIL = "new@example.com"
        private const val TEST_CODE = "123456"
    }
}