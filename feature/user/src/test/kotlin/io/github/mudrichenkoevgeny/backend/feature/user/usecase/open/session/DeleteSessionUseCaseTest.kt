package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.session

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeleteSessionUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val sessionManager = mockk<SessionManager>()
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)

    private val useCase = DeleteSessionUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        sessionManager = sessionManager,
        webSocketManager = webSocketManager
    )

    private fun createAuthContext() = AuthenticatedRequestContext(
        traceId = null,
        userId = UserId.generate(),
        userRole = UserRole.USER,
        sessionId = UserSessionId.generate(),
        clientInfo = ClientInfo()
    )

    @Test
    fun `successfully deletes specific session`() = runTest {
        val context = createAuthContext()
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
                actorUserRole = context.userRole.serialName,
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
        val context = createAuthContext()
        val targetSessionId = UserSessionId.generate()
        val error = mockk<AppError>()
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Error(error)
        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(targetSessionId, context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)

        coVerify {
            auditLogger.log(
                actorId = context.userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
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
        val context = createAuthContext()
        val targetSessionId = UserSessionId.generate()
        val error = mockk<AppError>()
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { sessionManager.deleteSessionById(targetSessionId) } returns AppResult.Error(error)
        every { auditErrorConverter.convert(error) } returns errorLogData

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
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.SELF_DELETE_SESSION,
                resource = UserAuditResourceType.SESSION,
                resourceId = targetSessionId.asHexDashString(),
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
    }
}