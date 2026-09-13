package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.audit

import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventId
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditValueSensitivity
import io.mockk.mockk
import kotlin.time.Clock

fun createTestAuditEvent(
    id: AuditEventId = AuditEventId.generate(),
    actorId: String = "test-actor-id"
): AuditEvent = AuditEvent(
    id = id,
    actorId = actorId,
    actorType = mockk(relaxed = true),
    actorUserRole = null,
    action = mockk(relaxed = true),
    resource = mockk(relaxed = true),
    resourceId = null,
    resourceValueSensitivity = AuditValueSensitivity.NON_SENSITIVE,
    status = mockk(relaxed = true),
    metadata = emptySet(),
    message = null,
    createdAt = Clock.System.now()
)