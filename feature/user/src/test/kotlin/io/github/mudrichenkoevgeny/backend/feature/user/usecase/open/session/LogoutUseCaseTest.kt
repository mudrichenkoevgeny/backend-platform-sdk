package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.session

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
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

class LogoutUseCaseTest {

    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val sessionManager = mockk<SessionManager>()
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)

    private val useCase = LogoutUseCase(
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        sessionManager = sessionManager,
        webSocketManager = webSocketManager
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
    fun `successfully logs out current session`() = runTest {
        coEvery { sessionManager.deleteSessionById(sessionId) } returns AppResult.Success(Unit)

        val result = useCase(context)

        assertEquals(AppResult.Success(Unit), result)

        coVerify {
            webSocketManager.sendMessageToUserSession(
                userSessionId = sessionId,
                frame = any()
            )
        }

        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.LOGOUT,
                resource = UserAuditResourceType.SESSION,
                resourceId = sessionId.asHexDashString(),
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when session deletion fails`() = runTest {
        val error = mockk<AppError>()
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { sessionManager.deleteSessionById(sessionId) } returns AppResult.Error(error)
        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)

        coVerify(exactly = 0) {
            webSocketManager.sendMessageToUserSession(any(), any())
        }

        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.LOGOUT,
                resource = UserAuditResourceType.SESSION,
                resourceId = sessionId.asHexDashString(),
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
    }
}