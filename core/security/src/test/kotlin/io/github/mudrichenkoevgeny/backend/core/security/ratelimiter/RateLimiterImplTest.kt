package io.github.mudrichenkoevgeny.backend.core.security.ratelimiter

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.manager.redis.RedisManager
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.model.RateLimitAction
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.naming.CommonErrorArgs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RateLimiterImplTest {

    private val redisManager = mockk<RedisManager>()
    private val appLogger = mockk<AppLogger>(relaxed = true)
    private val rateLimiter = RateLimiterImpl(redisManager, appLogger)

    @Test
    fun `isRateLimited returns Allowed when counter is within limit`() = runTest {
        val action = TestRateLimitAction.LOGIN_ATTEMPT
        val identifier = "user:123"
        val key = action.createKey(identifier)

        coEvery { redisManager.incrementWithExpiration(key, action.windowSeconds.toLong()) } returns AppResult.Success(action.limit.toLong())

        val result = rateLimiter.isRateLimited(action, identifier) as AppResult.Success

        assertTrue(result.data is RateLimitResult.Allowed)
        coVerify(exactly = 0) { redisManager.getTtl(any()) }
    }

    @Test
    fun `isRateLimited returns Exceeded with retryAfterSeconds from TTL when counter exceeds limit`() = runTest {
        val action = TestRateLimitAction.LOGIN_ATTEMPT
        val identifier = "ip:127.0.0.1"
        val key = action.createKey(identifier)

        coEvery { redisManager.incrementWithExpiration(key, action.windowSeconds.toLong()) } returns AppResult.Success((action.limit + 1).toLong())
        coEvery { redisManager.getTtl(key) } returns AppResult.Success(42L)

        val result = rateLimiter.isRateLimited(action, identifier) as AppResult.Success
        val exceeded = result.data as RateLimitResult.Exceeded

        assertEquals(42, exceeded.error.publicArgs?.get(CommonErrorArgs.RETRY_AFTER_SECONDS))
    }

    @Test
    fun `isRateLimited falls back to windowSeconds when TTL lookup fails`() = runTest {
        val action = TestRateLimitAction.PASSWORD_CHANGE
        val identifier = "user:42"
        val key = action.createKey(identifier)

        coEvery { redisManager.incrementWithExpiration(key, action.windowSeconds.toLong()) } returns AppResult.Success((action.limit + 1).toLong())
        coEvery { redisManager.getTtl(key) } returns AppResult.Error(CommonError.Internal(Throwable("ttl error")))

        val result = rateLimiter.isRateLimited(action, identifier) as AppResult.Success
        val exceeded = result.data as RateLimitResult.Exceeded

        assertEquals(action.windowSeconds, exceeded.error.publicArgs?.get(CommonErrorArgs.RETRY_AFTER_SECONDS))
    }

    private enum class TestRateLimitAction(
        override val id: String,
        override val limit: Int,
        override val windowSeconds: Int
    ) : RateLimitAction {
        LOGIN_ATTEMPT("login", limit = 5, windowSeconds = 60),
        PASSWORD_CHANGE("password_change", limit = 3, windowSeconds = 300)
    }
}

