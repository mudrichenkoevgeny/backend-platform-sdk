package io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.session

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.error.CommonAuditErrorParser
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.session.LogoutUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LogoutUseCaseTest {

    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val commonAuditErrorParser = CommonAuditErrorParser()
    private val auditErrorConverter = AuditErrorConverter(emptySet(), commonAuditErrorParser)
    private val sessionManager = mockk<SessionManager>()
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)

    private val useCase = LogoutUseCase(
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        sessionManager = sessionManager,
        webSocketManager = webSocketManager
    )

    private val context = createTestAuthenticatedRequestContext(userRole = UserRole.USER)
    private val userId = context.userId
    private val sessionId = context.sessionId

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
                actorUserRole = context.userRole.value,
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
        val error = CommonError.Unknown()

        coEvery { sessionManager.deleteSessionById(sessionId) } returns AppResult.Error(error)

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
                actorUserRole = context.userRole.value,
                action = UserAuditActionType.LOGOUT,
                resource = UserAuditResourceType.SESSION,
                resourceId = sessionId.asHexDashString(),
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
    }
}
