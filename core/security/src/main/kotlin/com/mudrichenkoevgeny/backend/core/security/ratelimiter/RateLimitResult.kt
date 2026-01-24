package com.mudrichenkoevgeny.backend.core.security.ratelimiter

import com.mudrichenkoevgeny.backend.core.common.error.model.CommonError

sealed class RateLimitResult {
    object Allowed : RateLimitResult()
    data class Exceeded(val error: CommonError.TooManyRequests) : RateLimitResult()
}