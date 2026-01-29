package io.github.mudrichenkoevgeny.backend.core.security.ratelimiter

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult

interface RateLimiter {
    suspend fun isRateLimited(
        action: RateLimitAction,
        identifier: String
    ): AppResult<RateLimitResult>
}