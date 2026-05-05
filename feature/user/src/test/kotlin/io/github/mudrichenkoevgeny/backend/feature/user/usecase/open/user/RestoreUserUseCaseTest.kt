package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
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

class RestoreUserUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()
    private val authenticationChallengeService = mockk<AuthenticationChallengeService>()

    private val useCase = RestoreUserUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        userManager = userManager,
        sessionManager = sessionManager,
        authenticationChallengeService = authenticationChallengeService
    )

    private val userId = UserId.generate()
    private val sessionId = UserSessionId.generate()
    private val context = AuthenticatedRequestContext(
        traceId = null,
        userId = userId,
        userRole = UserRole.USER,
        sessionId = sessionId,
        clientInfo = ClientInfo()
    )

    @Test
    fun `successfully restores user`() = runTest {
        val userDetails = mockk<UserDetails> {
            every { accountStatusBeforeDeletion } returns UserAccountStatus.ACTIVE
        }
        val userSessionInternal = mockk<UserSessionInternal>()

        coEvery { rateLimiter.checkRateLimit(UserRateLimitAction.USER_RESTORE, userId.asHexDashString()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSystem(sessionId) } returns AppResult.Success(userSessionInternal)
        coEvery { authenticationChallengeService.ensureSessionConfirmed(userDetails, userSessionInternal) } returns AppResult.Success(Unit)
        coEvery { userManager.restoreUserForSelf(userId, UserAccountStatus.ACTIVE) } returns AppResult.Success(userDetails)

        val result = useCase(context)

        assertEquals(AppResult.Success(userDetails), result)

        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.SELF_RESTORE_USER,
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when rate limit exceeded`() = runTest {
        val error = mockk<AppError>()
        every { auditErrorConverter.convert(error) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Error(error)

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.SELF_RESTORE_USER,
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when session is missing`() = runTest {
        val userDetails = mockk<UserDetails>()
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSystem(sessionId) } returns AppResult.Success(null)

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.InvalidSession)
    }

    @Test
    fun `returns error when mfa confirmation fails`() = runTest {
        val userDetails = mockk<UserDetails>()
        val userSessionInternal = mockk<UserSessionInternal>()
        val error = mockk<AppError>()

        every { auditErrorConverter.convert(error) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSystem(sessionId) } returns AppResult.Success(userSessionInternal)
        coEvery { authenticationChallengeService.ensureSessionConfirmed(any(), any()) } returns AppResult.Error(error)

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)
    }

    @Test
    fun `returns error when user restoration fails`() = runTest {
        val userDetails = mockk<UserDetails> {
            every { accountStatusBeforeDeletion } returns null
        }
        val userSessionInternal = mockk<UserSessionInternal>()
        val error = mockk<AppError>()

        every { auditErrorConverter.convert(error) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSystem(sessionId) } returns AppResult.Success(userSessionInternal)
        coEvery { authenticationChallengeService.ensureSessionConfirmed(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.restoreUserForSelf(any(), any()) } returns AppResult.Error(error)

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.SELF_RESTORE_USER,
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
    }
}