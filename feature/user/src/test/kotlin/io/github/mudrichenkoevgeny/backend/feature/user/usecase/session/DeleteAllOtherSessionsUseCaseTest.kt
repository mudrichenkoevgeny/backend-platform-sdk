package io.github.mudrichenkoevgeny.backend.feature.user.usecase.session

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserSessionId
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeleteAllOtherSessionsUseCaseTest {

    private val rateLimiterEnforcer = mockk<RateLimitEnforcer>()
    private val userAuditLogger = mockk<UserAuditLogger>(relaxed = true)
    private val sessionManager = mockk<SessionManager>()

    private val useCase = DeleteAllOtherSessionsUseCase(
        rateLimiterEnforcer = rateLimiterEnforcer,
        userAuditLogger = userAuditLogger,
        sessionManager = sessionManager
    )

    @Test
    fun `execute returns InvalidAccessToken when request context has no userId`() = runBlocking {
        val ctx = RequestContext(
            traceId = null,
            userId = null,
            sessionId = UserSessionId.generate(),
            clientInfo = CLIENT_INFO
        )

        val result = useCase.execute(requestContext = ctx)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.InvalidAccessToken)
    }

    @Test
    fun `execute returns InvalidSession when request context has no sessionId`() = runBlocking {
        val ctx = RequestContext(
            traceId = null,
            userId = UserId.generate(),
            sessionId = null,
            clientInfo = CLIENT_INFO
        )

        val result = useCase.execute(requestContext = ctx)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.InvalidSession)
    }

    @Test
    fun `execute returns success when session manager revokes other sessions`() = runBlocking {
        val userId = UserId.generate()
        val sessionId = UserSessionId.generate()
        val ctx = requestContext(userId, sessionId)
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns AppResult.Success(Unit)
        coEvery { sessionManager.revokeAllUserSessionsExceptOne(userId, sessionId) } returns AppResult.Success(Unit)

        val result = useCase.execute(requestContext = ctx)

        assertTrue(result is AppResult.Success)
    }

    private fun requestContext(userId: UserId, sessionId: UserSessionId) = RequestContext(
        traceId = null,
        userId = userId,
        sessionId = sessionId,
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
