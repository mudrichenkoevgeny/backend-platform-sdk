package io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter

import io.github.mudrichenkoevgeny.backend.core.audit.metadata.toAuditEventMetadataSet
import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditService
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.model.RateLimitAction
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventMetadataValueSensitivity
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

// todo move to security module ??
/**
 * Default [RateLimitEnforcer] implementation.
 *
 * Delegates the actual limiting decision to [RateLimiter]. When the limit is exceeded, writes an
 * audit event via [AuditService] with [AuditStatus.DENIED] and enriches it using client data from
 * [io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext] ([RateLimitAuditMetadata]).
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
        val isRateLimitedResult = rateLimiter.checkRateLimit(rateLimitAction, rateLimitIdentifier)

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
                                action = StringBackedAuditAction(auditAction),
                                resource = StringBackedAuditResource(auditResource),
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

/** Temporary: this file is slated for removal/refactor; do not move into `core/audit`. */
private data class StringBackedAuditAction(
    override val serialName: String
) : AuditActionType {
    override fun parseOrNull(value: String): AuditActionType? = StringBackedAuditAction(value)
    override fun parseOrThrow(value: String): AuditActionType = StringBackedAuditAction(value)
}

private data class StringBackedAuditResource(
    override val serialName: String
) : AuditResourceType {
    override fun parseOrNull(value: String): AuditResourceType? = StringBackedAuditResource(value)
    override fun parseOrThrow(value: String): AuditResourceType = StringBackedAuditResource(value)
}

