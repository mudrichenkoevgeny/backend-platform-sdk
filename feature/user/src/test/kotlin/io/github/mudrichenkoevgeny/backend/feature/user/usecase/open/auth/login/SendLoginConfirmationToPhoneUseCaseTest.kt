package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpConfirmationData
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.UserOtpVerificationType
import io.github.mudrichenkoevgeny.backend.feature.user.service.phone.PhoneService
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SendLoginConfirmationToPhoneUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val otpService = mockk<OtpService>()
    private val phoneService = mockk<PhoneService>()

    private val useCase = SendLoginConfirmationToPhoneUseCase(
        rateLimiter = rateLimiter,
        otpService = otpService,
        phoneService = phoneService
    )

    private fun createRequestContext() = RequestContext(
        traceId = null,
        userId = null,
        userRole = null,
        sessionId = null,
        clientInfo = ClientInfo()
    )

    @Test
    fun `successfully sends verification code`() = runTest {
        val context = createRequestContext()
        val otpConfirmation = mockk<OtpConfirmation>()
        val otpData = OtpConfirmationData(
            code = TEST_CODE,
            otpConfirmation = otpConfirmation
        )

        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.SEND_OTP_PHONE, TEST_PHONE)
        } returns AppResult.Success(Unit)

        coEvery {
            otpService.getOtp(TEST_PHONE, UserOtpVerificationType.PHONE_VERIFICATION)
        } returns AppResult.Success(otpData)

        coEvery {
            phoneService.sendVerificationCode(TEST_PHONE, TEST_CODE, any())
        } returns AppResult.Success(Unit)

        val result = useCase(TEST_PHONE, context)

        assertEquals(AppResult.Success(otpConfirmation), result)

        coVerify(exactly = 1) {
            phoneService.sendVerificationCode(TEST_PHONE, TEST_CODE, any())
        }
    }

    @Test
    fun `returns error when rate limit exceeded`() = runTest {
        val context = createRequestContext()
        val error = UserError.InvalidCredentials()

        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.SEND_OTP_PHONE, TEST_PHONE)
        } returns AppResult.Error(error)

        val result = useCase(TEST_PHONE, context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)

        coVerify(exactly = 0) { otpService.getOtp(any(), any()) }
    }

    @Test
    fun `returns error when otp service fails`() = runTest {
        val context = createRequestContext()
        val error = UserError.InvalidCredentials()

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { otpService.getOtp(any(), any()) } returns AppResult.Error(error)

        val result = useCase(TEST_PHONE, context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)
    }

    @Test
    fun `returns error when phone service fails to send sms`() = runTest {
        val context = createRequestContext()
        val error = UserError.CannotCreateUserIdentifier()
        val otpData = OtpConfirmationData(code = TEST_CODE, otpConfirmation = mockk())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { otpService.getOtp(any(), any()) } returns AppResult.Success(otpData)
        coEvery {
            phoneService.sendVerificationCode(any(), any(), any())
        } returns AppResult.Error(error)

        val result = useCase(TEST_PHONE, context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)
    }

    companion object {
        private const val TEST_PHONE = "+79991112233"
        private const val TEST_CODE = "555666"
        private const val TEST_LANG = "ru"
    }
}