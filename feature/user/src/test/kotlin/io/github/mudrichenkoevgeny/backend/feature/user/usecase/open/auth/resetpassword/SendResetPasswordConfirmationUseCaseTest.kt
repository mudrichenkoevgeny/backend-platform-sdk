package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.resetpassword

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.backend.feature.user.model.otp.OtpVerificationType
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.EmailService
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.OtpService
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SendResetPasswordConfirmationUseCaseTest {

    private val rateLimiterEnforcer = mockk<RateLimitEnforcer>()
    private val userAuditLogger = mockk<UserAuditLogger>(relaxed = true)
    private val otpService = mockk<OtpService>()
    private val emailService = mockk<EmailService>()
    private val identifierManager = mockk<IdentifierManager>()

    private val useCase = SendResetPasswordConfirmationUseCase(
        rateLimiterEnforcer = rateLimiterEnforcer,
        userAuditLogger = userAuditLogger,
        otpService = otpService,
        emailService = emailService,
        identifierManager = identifierManager
    )

    @Test
    fun `execute returns success with SendConfirmation when email registered and code sent`() = runBlocking {
        val ctx = requestContext()
        val identifier = mockk<UserIdentifier>(relaxed = true)
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns AppResult.Success(Unit)
        coEvery {
            identifierManager.getUserIdentifier(
                userAuthProvider = UserAuthProvider.EMAIL,
                identifier = EMAIL
            )
        } returns AppResult.Success(identifier)
        coEvery {
            otpService.getOtp(
                identifier = EMAIL,
                type = OtpVerificationType.EMAIL_PASSWORD_RESET
            )
        } returns AppResult.Success(CODE)
        coEvery {
            emailService.sendResetPasswordVerificationCode(EMAIL, CODE, ctx.clientInfo.language)
        } returns AppResult.Success(Unit)

        val result = useCase.execute(email = EMAIL, requestContext = ctx)

        assertTrue(result is AppResult.Success)
        assertEquals(SendResetPasswordConfirmationUseCase.RETRY_AFTER_SECONDS, (result as AppResult.Success).data.retryAfterSeconds)
    }

    private fun requestContext() = RequestContext(
        traceId = null,
        userId = null,
        sessionId = null,
        clientInfo = CLIENT_INFO
    )

    private companion object {
        const val EMAIL = "user@example.com"
        const val CODE = "123456"

        val CLIENT_INFO = ClientInfo(
            clientType = null,
            userAgent = null,
            ipAddress = null,
            language = "en",
            host = null,
            origin = null,
            deviceId = null,
            deviceName = null,
            appVersion = null,
            operationSystemVersion = null
        )
    }
}
