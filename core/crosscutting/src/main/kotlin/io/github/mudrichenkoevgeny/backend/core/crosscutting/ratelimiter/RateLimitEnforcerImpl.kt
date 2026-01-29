package io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter

import io.github.mudrichenkoevgeny.backend.core.audit.enums.AuditStatus
import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEvent
import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditService
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.toJsonElementMap
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitAction
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RateLimitEnforcerImpl @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditService: AuditService
) : RateLimitEnforcer {
    override suspend fun enforce(
        requestContext: RequestContext,
        rateLimitAction: RateLimitAction,
        rateLimitIdentifier: String,
        auditAction: String,
        auditResource: String,
        auditResourceId: String?
    ): AppResult<Unit> {
        val isRateLimitedResult = rateLimiter.isRateLimited(rateLimitAction, rateLimitIdentifier)

        return when (isRateLimitedResult) {
            is AppResult.Success -> {
                val rateLimitResult = isRateLimitedResult.data
                when (rateLimitResult) {
                    is RateLimitResult.Allowed -> AppResult.Success(Unit)
                    is RateLimitResult.Exceeded -> {
                        auditService.log(
                            AuditEvent(
                                actorId = requestContext.userId?.value,
                                action = auditAction,
                                resource = auditResource,
                                resourceId = auditResourceId ?: "unknown",
                                status = AuditStatus.DENIED,
                                metadata = mapOf(
                                    RateLimitAuditMetadata.Keys.IP_ADDRESS to requestContext.clientInfo.ipAddress,
                                    RateLimitAuditMetadata.Keys.DEVICE_ID to requestContext.clientInfo.deviceId,
                                    RateLimitAuditMetadata.Keys.CLIENT_TYPE to requestContext.clientInfo.clientType,
                                    RateLimitAuditMetadata.Keys.USER_AGENT to requestContext.clientInfo.userAgent,
                                    RateLimitAuditMetadata.Keys.REASON to RateLimitAuditMetadata.Reasons.RATE_LIMIT
                                ).toJsonElementMap()
                            )
                        )
                        AppResult.Error(rateLimitResult.error)
                    }
                }
            }
            is AppResult.Error -> isRateLimitedResult
        }
    }
}