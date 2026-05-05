package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.resetpassword

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.core.security.usecase.open.passwordpolicy.ValidatePasswordUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.UserOtpVerificationType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ResetPasswordUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val otpService = mockk<OtpService>()
    private val identifierManager = mockk<IdentifierManager>()
    private val validatePasswordUseCase = mockk<ValidatePasswordUseCase>()

    private val useCase = ResetPasswordUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        otpService = otpService,
        identifierManager = identifierManager,
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
    fun `successfully resets password and logs audit`() = runTest {
        val context = createRequestContext()
        val userId = UserId.generate()
        val identifierInternal = mockk<UserIdentifierInternal> {
            every { this@mockk.userId } returns userId
        }
        val userIdentifier = mockk<UserIdentifier>()

        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.PASSWORD_CHANGE, TEST_EMAIL)
        } returns AppResult.Success(Unit)
        coEvery { validatePasswordUseCase(TEST_PASSWORD) } returns AppResult.Success(Unit)
        coEvery {
            otpService.verifyOtp(TEST_EMAIL, UserOtpVerificationType.EMAIL_PASSWORD_RESET, TEST_CODE)
        } returns AppResult.Success(true)
        coEvery {
            identifierManager.getUserIdentifierInternalByProvider(UserAuthProvider.EMAIL, TEST_EMAIL)
        } returns AppResult.Success(identifierInternal)
        coEvery {
            identifierManager.updateUserIdentifierPassword(identifierInternal, TEST_PASSWORD)
        } returns AppResult.Success(userIdentifier)

        val result = useCase(TEST_EMAIL, TEST_PASSWORD, TEST_CODE, context)

        assertTrue(result is AppResult.Success)
        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = null,
                action = UserAuditActionType.RESET_PASSWORD,
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.SUCCESS,
                message = null,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when otp code is incorrect`() = runTest {
        val context = createRequestContext()
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { validatePasswordUseCase(any()) } returns AppResult.Success(Unit)
        coEvery { otpService.verifyOtp(any(), any(), any()) } returns AppResult.Success(false)
        every {
            auditErrorConverter.convert(any<UserError.WrongConfirmationCode>())
        } returns errorLogData

        val result = useCase(TEST_EMAIL, TEST_PASSWORD, "wrong", context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.WrongConfirmationCode)
        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = null,
                actorType = AuditActorType.USER,
                actorUserRole = null,
                action = UserAuditActionType.RESET_PASSWORD,
                resource = UserAuditResourceType.USER,
                resourceId = null,
                status = AuditStatus.FAILED,
                message = null,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when user not found`() = runTest {
        val context = createRequestContext()
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { validatePasswordUseCase(any()) } returns AppResult.Success(Unit)
        coEvery { otpService.verifyOtp(any(), any(), any()) } returns AppResult.Success(true)
        coEvery {
            identifierManager.getUserIdentifierInternalByProvider(any(), any())
        } returns AppResult.Success(null)
        every { auditErrorConverter.convert(any<UserError.UserNotFound>()) } returns errorLogData

        val result = useCase(TEST_EMAIL, TEST_PASSWORD, TEST_CODE, context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserNotFound)
    }

    companion object {
        private const val TEST_EMAIL = "reset@example.com"
        private const val TEST_PASSWORD = "NewPassword123!"
        private const val TEST_CODE = "654321"
    }
}