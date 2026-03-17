package io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.model.RateLimitAction
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter

/**
 * Enforces rate limits for security-sensitive operations and optionally records an audit entry
 * when access is denied.
 *
 * This is a cross-cutting orchestrator that ties together:
 * - the low-level [RateLimiter] from `core/security`
 * - audit logging via `core/audit`
 *
 * The enforcer returns [AppResult.Success] when the operation is allowed, and [AppResult.Error]
 * when the operation is blocked or when a dependency fails.
 */
interface RateLimitEnforcer {
    /**
     * Checks rate limits for the given [rateLimitAction] and [rateLimitIdentifier].
     *
     * When the action is rate-limited, the implementation is expected to:
     * - write an audit event using [auditAction], [auditResource], and [auditResourceId]
     * - return [AppResult.Error] with the underlying "too many requests" error
     *
     * @param requestContext Request context used to enrich audit metadata (actor id, client info).
     * @param rateLimitAction Logical operation being rate limited.
     * @param rateLimitIdentifier A subject being limited (e.g. user id, email, phone, IP address).
     * @param auditAction Audit action name to record on denial.
     * @param auditResource Audit resource name to record on denial.
     * @param auditResourceId Optional audit resource id to record on denial.
     */
    suspend fun enforce(
        requestContext: RequestContext,
        rateLimitAction: RateLimitAction,
        rateLimitIdentifier: String,
        auditAction: String,
        auditResource: String,
        auditResourceId: String?
    ): AppResult<Unit>
}