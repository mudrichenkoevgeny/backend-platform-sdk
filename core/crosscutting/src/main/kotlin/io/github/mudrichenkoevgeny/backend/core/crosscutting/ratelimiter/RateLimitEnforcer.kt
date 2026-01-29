package io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitAction

interface RateLimitEnforcer {
    suspend fun enforce(
        requestContext: RequestContext,
        rateLimitAction: RateLimitAction,
        rateLimitIdentifier: String,
        auditAction: String,
        auditResource: String,
        auditResourceId: String?
    ): AppResult<Unit>
}