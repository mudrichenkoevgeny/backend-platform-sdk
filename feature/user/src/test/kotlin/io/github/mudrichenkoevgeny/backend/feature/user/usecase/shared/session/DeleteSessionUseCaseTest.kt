package io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.session

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.error.CommonAuditErrorParser
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.session.DeleteSessionUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeleteSessionUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val commonAuditErrorParser = CommonAuditErrorParser()
    private val auditErrorConverter = AuditErrorConverter(emptySet(), commonAuditErrorParser)
    private val sessionManager = mockk<SessionManager>()
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)

    private val useCase = DeleteSessionUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        sessionManager = sessionManager,
        webSocketManager = webSocketManager
    )

    @Test
    fun `successfully deletes specific session`() = runTest {
        val context = createTestAuthenticatedRequestContext(userRole = UserRole.USER)
        val targetSessionId = UserSessionId.generate()

        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.SESSION_DELETE, context.userId.asHexDashString())
        } returns AppResult.Success(Unit)

        coEvery {
            sessionManager.deleteSessionById(targetSessionId)
        } returns AppResult.Success(Unit)

        val result = useCase(targetSessionId, context)

        assertEquals(AppResult.Success(Unit), result)

        coVerify {
            webSocketManager.sendMessageToUserSession(
                userSessionId = targetSessionId,
                frame = any()
            )
        }

        coVerify {
            auditLogger.log(
                actorId = context.userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.value,
                action = UserAuditActionType.SELF_DELETE_SESSION,
                resource = UserAuditResourceType.SESSION,
                resourceId = targetSessionId.asHexDashString(),
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when rate limit exceeded`() = runTest {
        val context = createTestAuthenticatedRequestContext(userRole = UserRole.USER)
        val targetSessionId = UserSessionId.generate()
        val error = CommonError.Unknown()

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Error(error)

        val result = useCase(targetSessionId, context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)

        coVerify {
            auditLogger.log(
                actorId = context.userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.value,
                action = UserAuditActionType.SELF_DELETE_SESSION,
                resource = UserAuditResourceType.SESSION,
                resourceId = targetSessionId.asHexDashString(),
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }

        coVerify(exactly = 0) { sessionManager.deleteSessionById(any()) }
    }

    @Test
    fun `returns error when session manager fails to delete`() = runTest {
        val context = createTestAuthenticatedRequestContext(userRole = UserRole.USER)
        val targetSessionId = UserSessionId.generate()
        val error = CommonError.Unknown()

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { sessionManager.deleteSessionById(targetSessionId) } returns AppResult.Error(error)

        val result = useCase(targetSessionId, context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)

        coVerify(exactly = 0) {
            webSocketManager.sendMessageToUserSession(any(), any())
        }

        coVerify {
            auditLogger.log(
                actorId = context.userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.value,
                action = UserAuditActionType.SELF_DELETE_SESSION,
                resource = UserAuditResourceType.SESSION,
                resourceId = targetSessionId.asHexDashString(),
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
    }
}
