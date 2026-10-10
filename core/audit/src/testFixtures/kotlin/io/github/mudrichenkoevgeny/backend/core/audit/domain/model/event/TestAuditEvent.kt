package io.github.mudrichenkoevgeny.backend.core.audit.domain.model.event

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.action.RepositoryTestAuditAction
import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.resource.RepositoryTestAuditResource
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventId
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventInternal
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventPrivate
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventSummary
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditValueSensitivity
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import kotlin.time.Clock
import kotlin.time.Instant

fun createTestAuditEventInternal(
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
    createdAt: Instant = Clock.System.now(),
    updatedAt: Instant? = null
): AuditEventInternal = AuditEventInternal(
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
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun createTestAuditEventPrivate(
    id: AuditEventId = AuditEventId.generate(),
    actorId: String? = null,
    actorType: AuditActorType = AuditActorType.SYSTEM,
    actorUserRole: String? = null,
    action: AuditActionType = RepositoryTestAuditAction("test_action"),
    resource: AuditResourceType = RepositoryTestAuditResource("test_resource"),
    resourceId: String? = null,
    status: AuditStatus = AuditStatus.SUCCESS,
    metadata: Set<AuditEventMetadata> = emptySet(),
    message: String? = null,
    createdAt: Instant = Clock.System.now(),
    updatedAt: Instant? = null
): AuditEventPrivate = AuditEventPrivate(
    id = id,
    actorId = actorId,
    actorType = actorType,
    actorUserRole = actorUserRole,
    action = action,
    resource = resource,
    resourceId = resourceId,
    status = status,
    metadata = metadata,
    message = message,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun createTestAuditEventSummary(
    id: AuditEventId = AuditEventId.generate(),
    actorType: AuditActorType = AuditActorType.SYSTEM,
    action: AuditActionType = RepositoryTestAuditAction("test_action"),
    resource: AuditResourceType = RepositoryTestAuditResource("test_resource"),
    status: AuditStatus = AuditStatus.SUCCESS,
    createdAt: Instant = Clock.System.now()
): AuditEventSummary = AuditEventSummary(
    id = id,
    actorType = actorType,
    action = action,
    resource = resource,
    status = status,
    createdAt = createdAt
)
