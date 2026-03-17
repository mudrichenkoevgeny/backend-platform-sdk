package io.github.mudrichenkoevgeny.backend.core.security.ratelimiter

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError

/**
 * Result of a rate limit check.
 */
sealed class RateLimitResult {
    /**
     * The action is within the configured rate limit.
     */
    object Allowed : RateLimitResult()

    /**
     * The action exceeded the configured rate limit.
     *
     * @property error Error details to be returned to the client (includes retry-after).
     */
    data class Exceeded(val error: CommonError.TooManyRequests) : RateLimitResult()
}