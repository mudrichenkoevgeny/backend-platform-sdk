package io.github.mudrichenkoevgeny.backend.core.audit.logger

import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus

/**
 * Thin application API for **writing** audit events through [AuditService].
 */
interface AuditLogger {

    /**
     * Schedules a single audit record for background persistence.
     */
    fun log(
        actorId: String? = null,
        actorType: AuditActorType,
        actorUserRole: String? = null,
        action: AuditActionType,
        resource: AuditResourceType,
        resourceId: String? = null,
        status: AuditStatus,
        message: String? = null,
        metadata: Set<AuditEventMetadata> = emptySet()
    )
}
