package io.github.mudrichenkoevgeny.backend.core.security.ratelimiter

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.model.RateLimitAction

interface RateLimiter {
    suspend fun isRateLimited(
        action: RateLimitAction,
        identifier: String
    ): AppResult<RateLimitResult>
}