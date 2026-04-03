package io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter

import io.github.mudrichenkoevgeny.backend.core.audit.domain.wire.AuditWireAction
import io.github.mudrichenkoevgeny.backend.core.audit.domain.wire.AuditWireResource
import io.github.mudrichenkoevgeny.backend.core.audit.metadata.toAuditEventMetadataSet
import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditService
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.model.RateLimitAction
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventMetadataValueSensitivity
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * Default [RateLimitEnforcer] implementation.
 *
 * Delegates the actual limiting decision to [RateLimiter]. When the limit is exceeded, writes an
 * audit event via [AuditService] with [AuditStatus.DENIED] and enriches it using client data from
 * [RequestContext] ([RateLimitAuditMetadata]).
 */
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
                                actorId = requestContext.userId,
                                actorType = AuditActorType.USER,
                                actorUserRole = null,
                                action = AuditWireAction(auditAction),
                                resource = AuditWireResource(auditResource),
                                resourceId = auditResourceId ?: "unknown",
                                status = AuditStatus.DENIED,
                                metadata = mapOf(
                                    RateLimitAuditMetadata.Keys.IP_ADDRESS to requestContext.clientInfo.ipAddress,
                                    RateLimitAuditMetadata.Keys.DEVICE_ID to requestContext.clientInfo.deviceId?.value,
                                    RateLimitAuditMetadata.Keys.CLIENT_TYPE to requestContext.clientInfo.clientType,
                                    RateLimitAuditMetadata.Keys.USER_AGENT to requestContext.clientInfo.userAgent,
                                    RateLimitAuditMetadata.Keys.REASON to RateLimitAuditMetadata.Reasons.RATE_LIMIT
                                ).toAuditEventMetadataSet { key ->
                                    if (key == RateLimitAuditMetadata.Keys.IP_ADDRESS) {
                                        AuditEventMetadataValueSensitivity.IP_ADDRESS
                                    } else {
                                        AuditEventMetadataValueSensitivity.NON_SENSITIVE
                                    }
                                },
                                createdAt = Clock.System.now()
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
