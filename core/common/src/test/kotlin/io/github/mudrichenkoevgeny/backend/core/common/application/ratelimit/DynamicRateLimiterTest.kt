package io.github.mudrichenkoevgeny.backend.core.common.application.ratelimit

import io.ktor.server.plugins.ratelimit.RateLimiter
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DynamicRateLimiterTest {

    @Test
    fun `getLimiterForKey returns same instance for same key when limits unchanged`() {
        val limit = 100
        val period = 60

        val dynamicLimiter = DynamicRateLimiter(
            getMaxRequestsPerPeriod = { limit },
            getRateLimitPeriodSeconds = { period }
        )

        val limiter1 = dynamicLimiter.getLimiterForKey("127.0.0.1")
        val limiter2 = dynamicLimiter.getLimiterForKey("127.0.0.1")

        assertSame(limiter1, limiter2)
    }

    @Test
    fun `getLimiterForKey returns different instances for different keys`() {
        val limit = 100
        val period = 60

        val dynamicLimiter = DynamicRateLimiter(
            getMaxRequestsPerPeriod = { limit },
            getRateLimitPeriodSeconds = { period }
        )

        val limiter1 = dynamicLimiter.getLimiterForKey("127.0.0.1")
        val limiter2 = dynamicLimiter.getLimiterForKey("192.168.1.1")

        assertNotSame(limiter1, limiter2)
    }

    @Test
    fun `getLimiterForKey invalidates cache when maxRequestsPerPeriod changes`() {
        var limit = 100
        val period = 60

        val dynamicLimiter = DynamicRateLimiter(
            getMaxRequestsPerPeriod = { limit },
            getRateLimitPeriodSeconds = { period }
        )

        val limiterBefore = dynamicLimiter.getLimiterForKey("127.0.0.1")

        limit = 200

        val limiterAfter = dynamicLimiter.getLimiterForKey("127.0.0.1")

        assertNotSame(limiterBefore, limiterAfter)
    }

    @Test
    fun `getLimiterForKey invalidates cache when rateLimitPeriodSeconds changes`() {
        val limit = 100
        var period = 60

        val dynamicLimiter = DynamicRateLimiter(
            getMaxRequestsPerPeriod = { limit },
            getRateLimitPeriodSeconds = { period }
        )

        val limiterBefore = dynamicLimiter.getLimiterForKey("127.0.0.1")

        period = 120

        val limiterAfter = dynamicLimiter.getLimiterForKey("127.0.0.1")

        assertNotSame(limiterBefore, limiterAfter)
    }

    @Test
    fun `returned RateLimiter consumes tokens correctly`() = runTest {
        val dynamicLimiter = DynamicRateLimiter(
            getMaxRequestsPerPeriod = { 2 },
            getRateLimitPeriodSeconds = { 60 }
        )

        val limiter = dynamicLimiter.getLimiterForKey("client-key")

        val state1 = limiter.tryConsume(1)
        val state2 = limiter.tryConsume(1)

        assertTrue(state1 is RateLimiter.State.Available)
        assertTrue(state2 is RateLimiter.State.Available)
        assertEquals(1, (state1 as RateLimiter.State.Available).remainingTokens)
        assertEquals(0, (state2 as RateLimiter.State.Available).remainingTokens)
    }
}