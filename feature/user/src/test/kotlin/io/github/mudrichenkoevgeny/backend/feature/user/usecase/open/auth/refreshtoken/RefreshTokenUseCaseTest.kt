package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.refreshtoken

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider.RefreshTokenProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.AccessToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshTokenHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.SessionToken
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

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
    fun `returns error when getRefreshTokenHash fails`() = runTest {
        val refreshToken = RefreshToken("refresh-token")
        val ctx = createRequestContext()
        val hashError = AppResult.Error(CommonError.Internal(Throwable("hash failed")))

        every { refreshTokenProvider.getRefreshTokenHash(refreshToken) } returns hashError

        val result = useCase(refreshToken, ctx)

        assertEquals(hashError, result)
        coVerify(exactly = 0) { rateLimiter.checkRateLimit(any(), any()) }
        coVerify(exactly = 0) { sessionManager.refreshSession(any(), any()) }
    }

    @Test
    fun `returns rate limit error when checkRateLimit fails`() = runTest {
        val refreshToken = RefreshToken("refresh-token")
        val ctx = createRequestContext()
        val rateLimitError = AppResult.Error(
            CommonError.TooManyRequests(
                rateLimitActionCode = UserRateLimitAction.REFRESH_TOKEN.id,
                limit = UserRateLimitAction.REFRESH_TOKEN.limit,
                identifier = REFRESH_HASH_B64,
                retryAfterSeconds = 60
            )
        )

        every { refreshTokenProvider.getRefreshTokenHash(refreshToken) } returns AppResult.Success(
            RefreshTokenHash(REFRESH_HASH_B64)
        )
        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.REFRESH_TOKEN, REFRESH_HASH_B64)
        } returns rateLimitError

        val result = useCase(refreshToken, ctx)

        assertEquals(rateLimitError, result)
        coVerify(exactly = 0) { sessionManager.refreshSession(any(), any()) }
    }

    @Test
    fun `successfully refreshes session when all checks pass`() = runTest {
        val refreshToken = RefreshToken("old-refresh-token")
        val ctx = createRequestContext()
        val newSessionToken = SessionToken(
            accessToken = AccessToken("new-access"),
            refreshToken = RefreshToken("new-refresh"),
            expiresAt = Clock.System.now() + 1.hours
        )

        every { refreshTokenProvider.getRefreshTokenHash(refreshToken) } returns AppResult.Success(
            RefreshTokenHash(REFRESH_HASH_B64)
        )
        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.REFRESH_TOKEN, REFRESH_HASH_B64)
        } returns AppResult.Success(Unit)

        coEvery {
            sessionManager.refreshSession(
                refreshToken = refreshToken,
                clientInfo = ctx.clientInfo
            )
        } returns AppResult.Success(newSessionToken)

        val result = useCase(refreshToken, ctx)

        assertTrue(result is AppResult.Success)
        assertEquals(newSessionToken, (result as AppResult.Success).data)

        coVerify(exactly = 1) {
            sessionManager.refreshSession(refreshToken, ctx.clientInfo)
        }
    }

    @Test
    fun `returns error when session manager rejects refresh token`() = runTest {
        val refreshToken = RefreshToken("expired-or-invalid")
        val ctx = createRequestContext()
        val sessionError = AppResult.Error(UserError.InvalidRefreshToken())

        every { refreshTokenProvider.getRefreshTokenHash(refreshToken) } returns AppResult.Success(
            RefreshTokenHash(REFRESH_HASH_B64)
        )
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery {
            sessionManager.refreshSession(any(), any())
        } returns sessionError

        val result = useCase(refreshToken, ctx)

        assertEquals(sessionError, result)
    }

    private fun createRequestContext() = RequestContext(
        traceId = null,
        userId = null,
        userRole = null,
        sessionId = null,
        clientInfo = ClientInfo()
    )

    private companion object {
        const val REFRESH_HASH_B64 = "hashed_refresh_token_string"
    }
}