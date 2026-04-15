package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.session

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.session.UserSession
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.session.GetSessionsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetSessionsUseCaseTest {

    private val userAuditLogger = mockk<UserAuditLogger>(relaxed = true)
    private val sessionManager = mockk<SessionManager>()

    private val useCase = GetSessionsUseCase(
        userAuditLogger = userAuditLogger,
        sessionManager = sessionManager
    )

    @Test
    fun `execute returns InvalidAccessToken when request context has no userId`() = runBlocking {
        val ctx = requestContext(userId = null)

        val result = useCase.execute(requestContext = ctx)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.InvalidAccessToken)
    }

    @Test
    fun `execute returns success with sessions from session manager`() = runBlocking {
        val userId = UserId.generate()
        val ctx = requestContext(userId = userId)
        val sessions = emptyList<UserSession>()
        coEvery { sessionManager.getAllUserSessions(userId) } returns AppResult.Success(sessions)

        val result = useCase.execute(requestContext = ctx)

        assertTrue(result is AppResult.Success)
        assertEquals(sessions, (result as AppResult.Success).data)
    }

    private fun requestContext(userId: UserId?) = RequestContext(
        traceId = null,
        userId = userId,
        sessionId = null,
        clientInfo = CLIENT_INFO
    )

    private companion object {
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
