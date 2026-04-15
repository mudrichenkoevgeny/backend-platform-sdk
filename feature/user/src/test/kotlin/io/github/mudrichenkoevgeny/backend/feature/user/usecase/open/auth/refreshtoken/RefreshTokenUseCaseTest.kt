package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.refreshtoken

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider.RefreshTokenProvider
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.refreshtoken.RefreshTokenUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.AccessToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshTokenHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.SessionToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RefreshTokenUseCaseTest {

    private val sessionManager = mockk<SessionManager>()
    private val rateLimiter = mockk<RateLimiter>()
    private val refreshTokenProvider = mockk<RefreshTokenProvider>()

    private val useCase = RefreshTokenUseCase(
        sessionManager = sessionManager,
        rateLimiter = rateLimiter,
        refreshTokenProvider = refreshTokenProvider
    )

    @Test
    fun `execute returns error when getRefreshTokenHash fails`() = runBlocking {
        val refreshToken = RefreshToken("refresh-token")
        val ctx = requestContext()
        val hashError = AppResult.Error(CommonError.Internal(Throwable("hash failed")))
        every { refreshTokenProvider.getRefreshTokenHash(refreshToken) } returns hashError

        val result = useCase.execute(refreshToken = refreshToken, requestContext = ctx)

        assertEquals(hashError, result)
        coVerify(exactly = 0) { rateLimiter.checkRateLimit(any(), any()) }
        coVerify(exactly = 0) { sessionManager.refreshSession(any(), any(), any()) }
    }

    @Test
    fun `execute returns rate limit error when checkRateLimit fails`() = runBlocking {
        val refreshToken = RefreshToken("refresh-token")
        val ctx = requestContext()
        every { refreshTokenProvider.getRefreshTokenHash(refreshToken) } returns AppResult.Success(
            RefreshTokenHash(REFRESH_HASH_B64)
        )
        val rateLimitError = AppResult.Error(
            CommonError.TooManyRequests(
                rateLimitActionCode = UserRateLimitAction.REFRESH_TOKEN.id,
                limit = UserRateLimitAction.REFRESH_TOKEN.limit,
                identifier = "irrelevant",
                retryAfterSeconds = 42
            )
        )
        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.REFRESH_TOKEN, REFRESH_HASH_B64)
        } returns rateLimitError

        val result = useCase.execute(refreshToken = refreshToken, requestContext = ctx)

        assertEquals(rateLimitError, result)
        coVerify(exactly = 0) { sessionManager.refreshSession(any(), any(), any()) }
    }

    @Test
    fun `execute returns success when rate limit passes and session manager refreshes`() = runBlocking {
        val refreshToken = RefreshToken("refresh-token")
        val ctx = requestContext()
        every { refreshTokenProvider.getRefreshTokenHash(refreshToken) } returns AppResult.Success(
            RefreshTokenHash(REFRESH_HASH_B64)
        )
        coEvery { rateLimiter.checkRateLimit(UserRateLimitAction.REFRESH_TOKEN, REFRESH_HASH_B64) } returns
            AppResult.Success(Unit)
        val sessionToken = SessionToken(
            accessToken = AccessToken("access"),
            refreshToken = RefreshToken("new-refresh"),
            expiresAt = Clock.System.now() + 1.hours
        )
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
        coVerify(exactly = 1) {
            rateLimiter.checkRateLimit(UserRateLimitAction.REFRESH_TOKEN, REFRESH_HASH_B64)
        }
    }

    @Test
    fun `execute returns InvalidRefreshToken when session manager rejects token`() = runBlocking {
        val refreshToken = RefreshToken("bad-token")
        val ctx = requestContext()
        every { refreshTokenProvider.getRefreshTokenHash(refreshToken) } returns AppResult.Success(
            RefreshTokenHash(REFRESH_HASH_B64)
        )
        coEvery { rateLimiter.checkRateLimit(UserRateLimitAction.REFRESH_TOKEN, REFRESH_HASH_B64) } returns
            AppResult.Success(Unit)
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
        userRole = null,
        sessionId = UserSessionId.generate(),
        clientInfo = ClientInfo(
            deviceInfo = ClientDeviceInfo(
                deviceId = null,
                deviceName = null,
                clientType = null,
                language = null,
                appVersion = null,
                operationSystemVersion = null
            ),
            userAgent = null,
            ipAddress = "127.0.0.1",
            host = null,
            origin = null,
            apiVersion = null
        )
    )

    private companion object {
        const val REFRESH_HASH_B64 = "dGVzdC1oYXNoLXZhbHVlLWZvci1yYXRlLWxpbWl0LWtleQ=="
    }
}
