package io.github.mudrichenkoevgeny.backend.core.audit.database.table

import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditValueSensitivity
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.network.model.event.AuditEventMetadataPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import kotlinx.serialization.serializer
import org.jetbrains.exposed.v1.json.jsonb

/**
 * Exposed table mapping for [AuditEvent].
 * Inherits id, createdAt, updatedAt from [BaseTable].
 *
 * Schema is created by a Flyway migration in `db/migration/core/audit/`.
 * The app must include this path in its Flyway migration locations.
 */
object AuditEventsTable : BaseTable("audit_events") {
    val actorId = varchar("actor_id", BaseDbConstraints.DEFAULT_MAX_LENGTH).nullable()
    val actorType = enumerationByName("actor_type", BaseDbConstraints.ENUM_MAX_LENGTH, AuditActorType::class)
    val actorUserRole = varchar("actor_user_role", BaseDbConstraints.DEFAULT_MAX_LENGTH).nullable()
    val action = varchar("action", BaseDbConstraints.DEFAULT_MAX_LENGTH)
    val resource = varchar("resource", BaseDbConstraints.DEFAULT_MAX_LENGTH)
    val resourceId = varchar("resource_id", BaseDbConstraints.DEFAULT_MAX_LENGTH).nullable()
    val resourceValueSensitivity = enumerationByName(
        "resource_value_sensitivity",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        AuditValueSensitivity::class
    )
    val status = enumerationByName("status", BaseDbConstraints.ENUM_MAX_LENGTH, AuditStatus::class)
    val metadata = jsonb<Set<AuditEventMetadataPayload>>(
        "metadata",
        FoundationJson,
        serializer<Set<AuditEventMetadataPayload>>()
    )
    val message = text("message").nullable()
}