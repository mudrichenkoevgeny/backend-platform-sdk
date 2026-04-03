package io.github.mudrichenkoevgeny.backend.core.audit.logger

import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * Default [AuditLogger]: builds an [AuditEvent] and forwards it to [AuditService.log].
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
        status: AuditStatus,
        message: String?,
        metadata: Set<AuditEventMetadata>
    ) {
        auditService.log(
            AuditEvent(
                actorId = actorId,
                actorType = actorType,
                actorUserRole = actorUserRole,
                action = action,
                resource = resource,
                resourceId = resourceId,
                status = status,
                metadata = metadata,
                message = message,
                createdAt = Clock.System.now()
            )
        )
    }
}
