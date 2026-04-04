package io.github.mudrichenkoevgeny.backend.feature.user.usecase.auth.password

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.useridentifier.UserIdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.otp.OtpVerificationType
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.core.security.usecase.open.passwordpolicy.ValidatePasswordUseCase
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResetPasswordUseCaseTest {

    private val rateLimiterEnforcer = mockk<RateLimitEnforcer>()
    private val userAuditLogger = mockk<UserAuditLogger>(relaxed = true)
    private val otpService = mockk<OtpService>()
    private val userIdentifierManager = mockk<UserIdentifierManager>()
    private val validatePasswordUseCase = mockk<ValidatePasswordUseCase>()

    private val useCase = ResetPasswordUseCase(
        rateLimiterEnforcer = rateLimiterEnforcer,
        userAuditLogger = userAuditLogger,
        otpService = otpService,
        userIdentifierManager = userIdentifierManager,
        validatePasswordUseCase = validatePasswordUseCase
    )

    @Test
    fun `execute returns WrongConfirmationCode when OTP verification fails`() = runBlocking {
        val ctx = requestContext()
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns AppResult.Success(Unit)
        every { validatePasswordUseCase(NEW_PASSWORD) } returns AppResult.Success(Unit)
        coEvery {
            otpService.verifyOtp(
                identifier = EMAIL,
                type = OtpVerificationType.EMAIL_PASSWORD_RESET,
                code = CODE
            )
        } returns AppResult.Success(false)

        val result = useCase.execute(
            email = EMAIL,
            newPassword = NEW_PASSWORD,
            confirmationCode = CODE,
            requestContext = ctx
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.WrongConfirmationCode)
    }

    @Test
    fun `execute returns success when OTP valid and password updated`() = runBlocking {
        val ctx = requestContext()
        val userIdentifier = mockk<UserIdentifier>(relaxed = true)
        val updatedIdentifier = mockk<UserIdentifier>(relaxed = true)
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns AppResult.Success(Unit)
        every { validatePasswordUseCase(NEW_PASSWORD) } returns AppResult.Success(Unit)
        coEvery {
            otpService.verifyOtp(
                identifier = EMAIL,
                type = OtpVerificationType.EMAIL_PASSWORD_RESET,
                code = CODE
            )
        } returns AppResult.Success(true)
        coEvery {
            userIdentifierManager.getUserIdentifier(
                userAuthProvider = UserAuthProvider.EMAIL,
                identifier = EMAIL
            )
        } returns AppResult.Success(userIdentifier)
        coEvery {
            userIdentifierManager.updateUserIdentifierPassword(
                userIdentifier = userIdentifier,
                identifier = EMAIL,
                password = NEW_PASSWORD
            )
        } returns AppResult.Success(updatedIdentifier)

        val result = useCase.execute(
            email = EMAIL,
            newPassword = NEW_PASSWORD,
            confirmationCode = CODE,
            requestContext = ctx
        )

        assertTrue(result is AppResult.Success)
        assertTrue((result as AppResult.Success).data === updatedIdentifier)
    }

    private fun requestContext() = RequestContext(
        traceId = null,
        userId = null,
        sessionId = null,
        clientInfo = CLIENT_INFO
    )

    private companion object {
        const val EMAIL = "user@example.com"
        const val NEW_PASSWORD = "newPass456"
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
