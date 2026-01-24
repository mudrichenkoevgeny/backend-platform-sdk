package com.mudrichenkoevgeny.backend.core.security.ratelimiter

import com.mudrichenkoevgeny.backend.core.common.result.AppResult

interface RateLimiter {
    suspend fun isRateLimited(
        action: RateLimitAction,
        identifier: String
    ): AppResult<RateLimitResult>
}