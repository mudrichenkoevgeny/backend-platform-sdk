package io.github.mudrichenkoevgeny.backend.feature.user.audit.logger

import io.github.mudrichenkoevgeny.backend.core.audit.metadata.toAuditEventMetadataSet
import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditService
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventMetadataValueSensitivity
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * Default [UserAuditLogger] implementation that delegates persistence to [AuditService].
 *
 * The implementation:
 * - maps feature-level outcomes to [AuditStatus]
 * - builds [AuditEvent.metadata] from [RequestContext] client information and extra metadata
 * - assigns [AuditEventMetadata.valueSensitivity] per key for downstream masking
 */
@Singleton
class UserAuditLoggerImpl @Inject constructor(
    private val auditService: AuditService
) : UserAuditLogger {

    override fun logInternalError(
        requestContext: RequestContext,
        action: String,
        resource: String,
        resourceId: String?,
        metadata: Map<String, Any?>
    ) {
        auditService.log(
            AuditEvent(
                actorId = requestContext.userId,
                actorType = if (requestContext.userId != null) AuditActorType.USER else AuditActorType.SYSTEM,
                actorUserRole = null,
                action = StringBackedAuditAction(action),
                resource = StringBackedAuditResource(resource),
                resourceId = resourceId,
                status = AuditStatus.FAILED,
                metadata = buildMetadata(
                    requestContext = requestContext,
                    type = UserAuditMetadata.Types.INTERNAL_ERROR,
                    extra = metadata
                ),
                createdAt = Clock.System.now()
            )
        )
    }

    override fun logFail(
        requestContext: RequestContext,
        action: String,
        resource: String,
        resourceId: String?,
        type: String?,
        metadata: Map<String, Any?>
    ) {
        auditService.log(
            AuditEvent(
                actorId = requestContext.userId,
                actorType = if (requestContext.userId != null) AuditActorType.USER else AuditActorType.SYSTEM,
                actorUserRole = null,
                action = StringBackedAuditAction(action),
                resource = StringBackedAuditResource(resource),
                resourceId = resourceId,
                status = AuditStatus.FAILED,
                metadata = buildMetadata(requestContext = requestContext, type = type, extra = metadata),
                createdAt = Clock.System.now()
            )
        )
    }

    override fun logSuccess(
        requestContext: RequestContext,
        action: String,
        resource: String,
        resourceId: String?,
        type: String?,
        metadata: Map<String, Any?>
    ) {
        auditService.log(
            AuditEvent(
                actorId = requestContext.userId,
                actorType = if (requestContext.userId != null) AuditActorType.USER else AuditActorType.SYSTEM,
                actorUserRole = null,
                action = StringBackedAuditAction(action),
                resource = StringBackedAuditResource(resource),
                resourceId = resourceId,
                status = AuditStatus.SUCCESS,
                metadata = buildMetadata(requestContext = requestContext, type = type, extra = metadata),
                createdAt = Clock.System.now()
            )
        )
    }

    private fun buildMetadata(
        requestContext: RequestContext,
        type: String?,
        extra: Map<String, Any?>
    ): Set<AuditEventMetadata> = buildSet {
        requestContext.clientInfo.ipAddress?.let { ip ->
            add(
                AuditEventMetadata(
                    key = UserAuditMetadata.Keys.IP_ADDRESS,
                    value = ip,
                    valueSensitivity = AuditEventMetadataValueSensitivity.IP_ADDRESS
                )
            )
        }
        requestContext.clientInfo.deviceName?.let { name ->
            add(
                AuditEventMetadata(
                    key = UserAuditMetadata.Keys.DEVICE_NAME,
                    value = name,
                    valueSensitivity = AuditEventMetadataValueSensitivity.NON_SENSITIVE
                )
            )
        }
        type?.let { t ->
            add(
                AuditEventMetadata(
                    key = UserAuditMetadata.Keys.TYPE,
                    value = t,
                    valueSensitivity = AuditEventMetadataValueSensitivity.NON_SENSITIVE
                )
            )
        }
        addAll(extra.toAuditEventMetadataSet { key -> userAuditExtraMetadataSensitivity(key) })
    }

    private fun userAuditExtraMetadataSensitivity(key: String): AuditEventMetadataValueSensitivity =
        when (key) {
            UserAuditMetadata.Keys.EMAIL_MASK,
            UserAuditMetadata.Keys.PHONE_NUMBER_MASK,
            UserAuditMetadata.Keys.EXTERNAL_AUTH_PROVIDER_TOKEN_MASK ->
                AuditEventMetadataValueSensitivity.PARTIAL_VALUE_MASK
            UserAuditMetadata.Keys.EXTERNAL_AUTH_PROVIDER_TOKEN ->
                AuditEventMetadataValueSensitivity.FULL_VALUE_MASK
            UserAuditMetadata.Keys.IP_ADDRESS ->
                AuditEventMetadataValueSensitivity.IP_ADDRESS
            else -> AuditEventMetadataValueSensitivity.NON_SENSITIVE
        }
}

/** Temporary: this class will be replaced; do not move into `core/audit`. */
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

