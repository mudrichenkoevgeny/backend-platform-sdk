package io.github.mudrichenkoevgeny.backend.feature.user.usecase.session

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserSessionId
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
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

class LogoutFromCurrentSessionUseCaseTest {

    private val rateLimiterEnforcer = mockk<RateLimitEnforcer>()
    private val userAuditLogger = mockk<UserAuditLogger>(relaxed = true)
    private val sessionManager = mockk<SessionManager>()

    private val useCase = LogoutFromCurrentSessionUseCase(
        rateLimiterEnforcer = rateLimiterEnforcer,
        userAuditLogger = userAuditLogger,
        sessionManager = sessionManager
    )

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
    fun `execute returns success when session manager revokes session`() = runBlocking {
        val sessionId = UserSessionId.generate()
        val ctx = RequestContext(
            traceId = null,
            userId = UserId.generate(),
            sessionId = sessionId,
            clientInfo = CLIENT_INFO
        )
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns AppResult.Success(Unit)
        coEvery { sessionManager.revokeSessionById(sessionId) } returns AppResult.Success(Unit)

        val result = useCase.execute(requestContext = ctx)

        assertTrue(result is AppResult.Success)
    }

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
