package io.github.mudrichenkoevgeny.backend.core.audit.logger

import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventInternal
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditValueSensitivity
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * Default [AuditLogger]: builds an [AuditEventInternal] and forwards it to [AuditService.log].
 */
@Singleton
class AuditLoggerImpl @Inject constructor(
    private val auditService: AuditService
) : AuditLogger {

    override fun log(
        actorId: String?,
        actorType: AuditActorType,
        actorUserRole: String?,
        action: AuditActionType,
        resource: AuditResourceType,
        resourceId: String?,
        resourceValueSensitivity: AuditValueSensitivity,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>,
        message: String?
    ) {
        auditService.log(
            AuditEventInternal(
                actorId = actorId,
                actorType = actorType,
                actorUserRole = actorUserRole,
                action = action,
                resource = resource,
                resourceId = resourceId,
                resourceValueSensitivity = resourceValueSensitivity,
                status = status,
                metadata = metadata,
                message = message,
                createdAt = Clock.System.now(),
                updatedAt = null
            )
        )
    }
}
