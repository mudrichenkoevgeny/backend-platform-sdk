package io.github.mudrichenkoevgeny.backend.core.audit.domain.model.event

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.action.RepositoryTestAuditAction
import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.resource.RepositoryTestAuditResource
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventId
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditValueSensitivity
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import kotlin.time.Clock
import kotlin.time.Instant

fun createTestAuditEvent(
    id: AuditEventId = AuditEventId.generate(),
    actorId: String? = null,
    actorType: AuditActorType = AuditActorType.SYSTEM,
    actorUserRole: String? = null,
    action: AuditActionType = RepositoryTestAuditAction("test_action"),
    resource: AuditResourceType = RepositoryTestAuditResource("test_resource"),
    resourceId: String? = null,
    resourceValueSensitivity: AuditValueSensitivity = AuditValueSensitivity.NON_SENSITIVE,
    status: AuditStatus = AuditStatus.SUCCESS,
    metadata: Set<AuditEventMetadata> = emptySet(),
    message: String? = null,
    createdAt: Instant = Clock.System.now()
): AuditEvent = AuditEvent(
    id = id,
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
    createdAt = createdAt
)
