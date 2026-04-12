package io.github.mudrichenkoevgeny.backend.feature.user.usecase.auth.refreshtoken

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserSessionId
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AccessToken
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshToken
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.SessionToken
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class RefreshTokenUseCaseTest {

    private val rateLimiterEnforcer = mockk<RateLimitEnforcer>()
    private val userAuditLogger = mockk<UserAuditLogger>(relaxed = true)
    private val sessionManager = mockk<SessionManager>()

    private val useCase = RefreshTokenUseCase(
        rateLimiterEnforcer = rateLimiterEnforcer,
        userAuditLogger = userAuditLogger,
        sessionManager = sessionManager
    )

    @Test
    fun `execute returns rate limit error when enforcer fails`() = runBlocking {
        val refreshToken = RefreshToken("refresh-token")
        val ctx = requestContext()
        val rateLimitError = AppResult.Error(UserError.InvalidAccessToken())
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns rateLimitError

        val result = useCase.execute(refreshToken = refreshToken, requestContext = ctx)

        assertTrue(result is AppResult.Error)
        assertEquals(rateLimitError, result)
    }

    @Test
    fun `execute returns success with session token when session manager refreshes`() = runBlocking {
        val refreshToken = RefreshToken("refresh-token")
        val ctx = requestContext()
        val sessionToken = SessionToken(
            accessToken = AccessToken("access"),
            refreshToken = RefreshToken("new-refresh"),
            expiresAt = Instant.now().plusSeconds(3600)
        )
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns AppResult.Success(Unit)
        coEvery {
            sessionManager.refreshSession(
                userId = ctx.userId,
                refreshToken = refreshToken,
                clientInfo = ctx.clientInfo
            )
        } returns AppResult.Success(sessionToken)

        val result = useCase.execute(refreshToken = refreshToken, requestContext = ctx)

        assertTrue(result is AppResult.Success)
        assertEquals(sessionToken, (result as AppResult.Success).data)
    }

    @Test
    fun `execute returns InvalidRefreshToken when session manager returns invalid token`() = runBlocking {
        val refreshToken = RefreshToken("bad-token")
        val ctx = requestContext()
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns AppResult.Success(Unit)
        coEvery {
            sessionManager.refreshSession(userId = any(), refreshToken = any(), clientInfo = any())
        } returns AppResult.Error(UserError.InvalidRefreshToken())

        val result = useCase.execute(refreshToken = refreshToken, requestContext = ctx)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.InvalidRefreshToken)
    }

    private fun requestContext() = RequestContext(
        traceId = null,
        userId = UserId.generate(),
        sessionId = UserSessionId.generate(),
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
