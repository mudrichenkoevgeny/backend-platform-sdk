package io.github.mudrichenkoevgeny.backend.feature.user.usecase.auth.login

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.model.otp.OtpVerificationType
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.feature.user.service.phone.PhoneService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SendLoginConfirmationToPhoneUseCaseTest {

    private val rateLimiterEnforcer = mockk<RateLimitEnforcer>()
    private val userAuditLogger = mockk<UserAuditLogger>(relaxed = true)
    private val otpService = mockk<OtpService>()
    private val phoneService = mockk<PhoneService>()

    private val useCase = SendLoginConfirmationToPhoneUseCase(
        rateLimiterEnforcer = rateLimiterEnforcer,
        userAuditLogger = userAuditLogger,
        otpService = otpService,
        phoneService = phoneService
    )

    @Test
    fun `execute returns success with SendConfirmation when OTP sent`() = runBlocking {
        val ctx = requestContext()
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns AppResult.Success(Unit)
        coEvery {
            otpService.getOtp(
                identifier = PHONE,
                type = OtpVerificationType.PHONE_VERIFICATION
            )
        } returns AppResult.Success(CODE)
        every { phoneService.sendVerificationCode(PHONE, CODE) } returns AppResult.Success(Unit)

        val result = useCase.execute(phoneNumber = PHONE, requestContext = ctx)

        assertTrue(result is AppResult.Success)
        assertEquals(SendLoginConfirmationToPhoneUseCase.RETRY_AFTER_SECONDS, (result as AppResult.Success).data.retryAfterSeconds)
    }

    private fun requestContext() = RequestContext(
        traceId = null,
        userId = null,
        sessionId = null,
        clientInfo = CLIENT_INFO
    )

    private companion object {
        const val PHONE = "+79001234567"
        const val CODE = "123456"

        val CLIENT_INFO = ClientInfo(
            clientType = null,
            userAgent = null,
            ipAddress = null,
            language = null,
            host = null,
            origin = null,
            deviceId = null,
            deviceName = null,
            appVersion = null,
            operationSystemVersion = null
        )
    }
}
