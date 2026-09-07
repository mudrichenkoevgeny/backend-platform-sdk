package io.github.mudrichenkoevgeny.backend.core.common.application.ratelimit

import io.ktor.server.plugins.ratelimit.RateLimiter
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

/**
 * Thread-safe rate limiter provider that dynamically resolves rate limiters per request key.
 *
 * Automatically detects changes in [getMaxRequestsPerPeriod] and [getRateLimitPeriodSeconds]
 * at runtime and recreates limiters when limits are updated.
 *
 * @param getMaxRequestsPerPeriod supplier function returning the maximum allowed requests per period
 * @param getRateLimitPeriodSeconds supplier function returning the rate limit period in seconds
 */
class DynamicRateLimiter(
    private val getMaxRequestsPerPeriod: () -> Int,
    private val getRateLimitPeriodSeconds: () -> Int
) {
    @Volatile
    private var currentLimit = getMaxRequestsPerPeriod()

    @Volatile
    private var currentPeriod = getRateLimitPeriodSeconds()

    private val limiters = ConcurrentHashMap<Any, RateLimiter>()

    /**
     * Returns a [RateLimiter] instance for the specified request [key].
     *
     * If the rate limit configuration has changed since the last invocation,
     * existing limiters are invalidated and new ones are created with the updated limits.
     *
     * @param key subject identifier (e.g. client IP address or user ID)
     * @return [RateLimiter] configured with the current rate limits
     */
    fun getLimiterForKey(key: Any): RateLimiter {
        val newLimit = getMaxRequestsPerPeriod()
        val newPeriod = getRateLimitPeriodSeconds()

        if (newLimit != currentLimit || newPeriod != currentPeriod) {
            synchronized(this) {
                if (newLimit != currentLimit || newPeriod != currentPeriod) {
                    currentLimit = newLimit
                    currentPeriod = newPeriod
                    limiters.clear()
                }
            }
        }

        return limiters.computeIfAbsent(key) {
            RateLimiter.default(
                limit = currentLimit,
                refillPeriod = currentPeriod.seconds
            )
        }
    }
}