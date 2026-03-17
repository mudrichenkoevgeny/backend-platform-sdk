package io.github.mudrichenkoevgeny.backend.core.security.ratelimiter

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.model.RateLimitAction

/**
 * Rate limiting service for security-sensitive operations (login, OTP, password change, etc.).
 *
 * Implementations typically use a shared store (e.g. Redis) to keep counters across instances.
 */
interface RateLimiter {
    /**
     * Checks whether [identifier] exceeded the limit for the given [action].
     *
     * @param action Logical operation being rate limited (defines limit and time window).
     * @param identifier A subject being limited (e.g. user id, email, phone, IP address).
     * @return [AppResult.Success] with [RateLimitResult]. On errors, returns [AppResult.Error].
     */
    suspend fun isRateLimited(
        action: RateLimitAction,
        identifier: String
    ): AppResult<RateLimitResult>
}