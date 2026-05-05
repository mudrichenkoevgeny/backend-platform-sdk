package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.core.security.usecase.open.passwordpolicy.ValidatePasswordUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.UserOtpVerificationType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
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

class AddUserIdentifierEmailUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()
    private val authManager = mockk<AuthManager>()
    private val otpService = mockk<OtpService>()
    private val authenticationChallengeService = mockk<AuthenticationChallengeService>()
    private val validatePasswordUseCase = mockk<ValidatePasswordUseCase>()

    private val useCase = AddUserIdentifierEmailUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        userManager = userManager,
        sessionManager = sessionManager,
        authManager = authManager,
        otpService = otpService,
        authenticationChallengeService = authenticationChallengeService,
        validatePasswordUseCase = validatePasswordUseCase
    )

    private fun createAuthContext() = AuthenticatedRequestContext(
        traceId = null,
        userId = UserId.generate(),
        userRole = UserRole.USER,
        sessionId = UserSessionId.generate(),
        clientInfo = ClientInfo()
    )

    @Test
    fun `successfully adds email identifier`() = runTest {
        val context = createAuthContext()
        val userDetails = mockk<UserDetails>()
        val sessionInternal = mockk<UserSessionInternal>()
        val identifierId = UserIdentifierId.generate()
        val identifier = mockk<UserIdentifier> {
            every { id } returns identifierId
        }

        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.USER_IDENTIFIER_ADD, any())
        } returns AppResult.Success(Unit)

        coEvery {
            userManager.getUserByIdForSelf(context.userId)
        } returns AppResult.Success(userDetails)

        coEvery {
            sessionManager.getUserSessionForSystem(context.sessionId)
        } returns AppResult.Success(sessionInternal)

        coEvery {
            authenticationChallengeService.ensureSessionConfirmed(userDetails, sessionInternal)
        } returns AppResult.Success(Unit)

        coEvery { validatePasswordUseCase(TEST_PASSWORD) } returns AppResult.Success(Unit)

        coEvery {
            otpService.verifyOtp(TEST_EMAIL, UserOtpVerificationType.EMAIL_VERIFICATION, TEST_CODE)
        } returns AppResult.Success(true)

        coEvery {
            authManager.createIdentifierForAuthorizedUser(
                context.userId, UserAuthProvider.EMAIL, TEST_EMAIL, TEST_PASSWORD, null
            )
        } returns AppResult.Success(identifier)

        val result = useCase(TEST_EMAIL, TEST_PASSWORD, TEST_CODE, context)

        assertEquals(AppResult.Success(identifier), result)
        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = context.userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.ADD_IDENTIFIER_EMAIL,
                resource = UserAuditResourceType.IDENTIFIER,
                resourceId = identifierId.asHexDashString(),
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when session confirmation fails`() = runTest {
        val context = createAuthContext()
        val userDetails = mockk<UserDetails>()
        val sessionInternal = mockk<UserSessionInternal>()
        val error = CommonError.Internal(Throwable())
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(any()) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSystem(any()) } returns AppResult.Success(sessionInternal)

        coEvery {
            authenticationChallengeService.ensureSessionConfirmed(any(), any())
        } returns AppResult.Error(error)

        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(TEST_EMAIL, TEST_PASSWORD, TEST_CODE, context)

        assertTrue(result is AppResult.Error)
        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = context.userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.ADD_IDENTIFIER_EMAIL,
                resource = UserAuditResourceType.IDENTIFIER,
                resourceId = null,
                status = AuditStatus.FAILED,
                message = null,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when rate limit exceeded`() = runTest {
        val context = createAuthContext()
        val error = UserError.InvalidCredentials()
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery {
            rateLimiter.checkRateLimit(any(), any())
        } returns AppResult.Error(error)

        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(TEST_EMAIL, TEST_PASSWORD, TEST_CODE, context)

        assertTrue(result is AppResult.Error)
        coVerify(exactly = 0) { userManager.getUserByIdForSelf(any()) }
    }

    companion object {
        private const val TEST_EMAIL = "new@example.com"
        private const val TEST_PASSWORD = "SafePassword123!"
        private const val TEST_CODE = "123456"
    }
}