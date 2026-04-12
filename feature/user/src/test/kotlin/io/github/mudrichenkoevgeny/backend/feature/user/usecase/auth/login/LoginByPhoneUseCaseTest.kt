package io.github.mudrichenkoevgeny.backend.feature.user.usecase.auth.login

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthData
import io.github.mudrichenkoevgeny.backend.feature.user.model.otp.OtpVerificationType
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.OtpService
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LoginByPhoneUseCaseTest {

    private val rateLimiterEnforcer = mockk<RateLimitEnforcer>()
    private val userAuditLogger = mockk<UserAuditLogger>(relaxed = true)
    private val otpService = mockk<OtpService>()
    private val authManager = mockk<AuthManager>()

    private val useCase = LoginByPhoneUseCase(
        rateLimiterEnforcer = rateLimiterEnforcer,
        userAuditLogger = userAuditLogger,
        otpService = otpService,
        authManager = authManager
    )

    @Test
    fun `execute returns WrongConfirmationCode when OTP verification fails`() = runBlocking {
        val ctx = requestContext()
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns AppResult.Success(Unit)
        coEvery {
            otpService.verifyOtp(
                identifier = PHONE,
                type = OtpVerificationType.PHONE_VERIFICATION,
                code = CODE
            )
        } returns AppResult.Success(false)

        val result = useCase.execute(
            phoneNumber = PHONE,
            confirmationCode = CODE,
            requestContext = ctx
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.WrongConfirmationCode)
    }

    @Test
    fun `execute returns success with auth data when OTP valid`() = runBlocking {
        val ctx = requestContext()
        val userIdentifier = mockk<UserIdentifier>(relaxed = true)
        val authData = mockk<AuthData>(relaxed = true)
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns AppResult.Success(Unit)
        coEvery {
            otpService.verifyOtp(
                identifier = PHONE,
                type = OtpVerificationType.PHONE_VERIFICATION,
                code = CODE
            )
        } returns AppResult.Success(true)
        coEvery {
            authManager.getOrCreateUserIdentifier(
                userAuthProvider = UserAuthProvider.PHONE,
                identifier = PHONE,
                userRole = UserRole.USER
            )
        } returns AppResult.Success(userIdentifier)
        coEvery {
            authManager.provideAuthData(
                userIdentifier = userIdentifier,
                clientInfo = ctx.clientInfo,
                allowedRoles = any()
            )
        } returns AppResult.Success(authData)

        val result = useCase.execute(
            phoneNumber = PHONE,
            confirmationCode = CODE,
            requestContext = ctx
        )

        assertTrue(result is AppResult.Success)
        assertTrue((result as AppResult.Success).data === authData)
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
