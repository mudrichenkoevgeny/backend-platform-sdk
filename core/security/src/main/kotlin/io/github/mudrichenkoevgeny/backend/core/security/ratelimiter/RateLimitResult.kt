package io.github.mudrichenkoevgeny.backend.core.security.ratelimiter

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError

sealed class RateLimitResult {
    object Allowed : RateLimitResult()
    data class Exceeded(val error: CommonError.TooManyRequests) : RateLimitResult()
}