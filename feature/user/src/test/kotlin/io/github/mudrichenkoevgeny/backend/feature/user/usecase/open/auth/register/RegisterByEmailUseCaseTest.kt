package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.register

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.core.security.usecase.open.passwordpolicy.ValidatePasswordUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.UserOtpVerificationType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.data.AuthData
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RegisterByEmailUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val otpService = mockk<OtpService>()
    private val authManager = mockk<AuthManager>()
    private val validatePasswordUseCase = mockk<ValidatePasswordUseCase>()

    private val useCase = RegisterByEmailUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        otpService = otpService,
        authManager = authManager,
        validatePasswordUseCase = validatePasswordUseCase
    )

    private fun createRequestContext() = RequestContext(
        traceId = null,
        userId = null,
        userRole = null,
        sessionId = null,
        clientInfo = ClientInfo()
    )

    @Test
    fun `successfully registers user and logs audit`() = runTest {
        val context = createRequestContext()
        val userId = UserId.generate()
        val userDetails = mockk<UserDetails> {
            every { id } returns userId
            every { role } returns UserRole.USER
        }
        val authData = mockk<AuthData> {
            every { this@mockk.userDetails } returns userDetails
        }

        coEvery { rateLimiter.checkRateLimit(UserRateLimitAction.REGISTRATION_ATTEMPT, TEST_EMAIL) } returns AppResult.Success(Unit)
        coEvery { validatePasswordUseCase(TEST_PASSWORD) } returns AppResult.Success(Unit)
        coEvery { otpService.verifyOtp(TEST_EMAIL, UserOtpVerificationType.EMAIL_VERIFICATION, TEST_CODE) } returns AppResult.Success(true)
        coEvery {
            authManager.authenticateOrCreateUser(any(), UserAuthProvider.EMAIL, TEST_EMAIL, TEST_PASSWORD)
        } returns AppResult.Success(authData)

        val result = useCase(TEST_EMAIL, TEST_PASSWORD, TEST_CODE, context)

        assertEquals(AppResult.Success(authData), result)

        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.USER.serialName,
                action = UserAuditActionType.REGISTER_BY_EMAIL,
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.SUCCESS,
                message = null,
                metadata = match { meta -> meta.any { it.value == TEST_EMAIL } }
            )
        }
    }

    @Test
    fun `returns error when password policy validation fails`() = runTest {
        val context = createRequestContext()
        val error = UserError.InvalidCredentials()
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { validatePasswordUseCase(any()) } returns AppResult.Error(error)
        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(TEST_EMAIL, "weak", TEST_CODE, context)

        assertTrue(result is AppResult.Error)
        coVerify(exactly = 1) {
            auditLogger.log(
                action = UserAuditActionType.REGISTER_BY_EMAIL,
                status = AuditStatus.FAILED,
                message = null,
                actorType = AuditActorType.USER,
                resource = UserAuditResourceType.USER,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when OTP code is incorrect`() = runTest {
        val context = createRequestContext()
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { validatePasswordUseCase(any()) } returns AppResult.Success(Unit)
        coEvery { otpService.verifyOtp(any(), any(), any()) } returns AppResult.Success(false)
        every { auditErrorConverter.convert(any<UserError.WrongConfirmationCode>()) } returns errorLogData

        val result = useCase(TEST_EMAIL, TEST_PASSWORD, "0000", context)

        assertTrue(result is AppResult.Error)
        coVerify(exactly = 1) {
            auditLogger.log(
                status = AuditStatus.FAILED,
                action = UserAuditActionType.REGISTER_BY_EMAIL,
                message = null,
                actorType = AuditActorType.USER,
                resource = UserAuditResourceType.USER,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when rate limit exceeded`() = runTest {
        val context = createRequestContext()
        val error = UserError.InvalidCredentials()
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Error(error)
        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(TEST_EMAIL, TEST_PASSWORD, TEST_CODE, context)

        assertTrue(result is AppResult.Error)
        coVerify(exactly = 0) { otpService.verifyOtp(any(), any(), any()) }
    }

    companion object {
        private const val TEST_EMAIL = "test@example.com"
        private const val TEST_PASSWORD = "Password123!"
        private const val TEST_CODE = "123456"
    }
}