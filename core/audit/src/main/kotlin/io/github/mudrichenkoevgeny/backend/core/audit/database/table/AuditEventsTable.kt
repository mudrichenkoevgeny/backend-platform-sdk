package io.github.mudrichenkoevgeny.backend.core.audit.database.table

import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventInternal
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditValueSensitivity
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.network.model.event.AuditEventMetadataPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import kotlinx.serialization.serializer
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.json.jsonb

/**
 * Exposed table mapping for [AuditEventInternal].
 * Inherits id, createdAt, updatedAt from [BaseTable].
 *
 * Schema is created by a Flyway migration in `db/migration/core/audit/`.
 * The app must include this path in its Flyway migration locations.
 */
public object AuditEventsTable : BaseTable("audit_events") {
    public val actorId: Column<String?> = varchar(
        "actor_id",
        BaseDbConstraints.DEFAULT_MAX_LENGTH
    ).nullable()
    public val actorType: Column<AuditActorType> = enumerationByName(
        "actor_type",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        AuditActorType::class
    )
    public val actorUserRole: Column<String?> = varchar(
        "actor_user_role",
        BaseDbConstraints.DEFAULT_MAX_LENGTH
    ).nullable()
    public val action: Column<String> = varchar(
        "action",
        BaseDbConstraints.DEFAULT_MAX_LENGTH
    )
    public val resource: Column<String> = varchar(
        "resource",
        BaseDbConstraints.DEFAULT_MAX_LENGTH
    )
    public val resourceId: Column<String?> = varchar(
        "resource_id",
        BaseDbConstraints.DEFAULT_MAX_LENGTH
    ).nullable()
    public val resourceValueSensitivity: Column<AuditValueSensitivity> = enumerationByName(
        "resource_value_sensitivity",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        AuditValueSensitivity::class
    )
    public val status: Column<AuditStatus> = enumerationByName(
        "status",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        AuditStatus::class
    )
    public val metadata: Column<Set<AuditEventMetadataPayload>> = jsonb<Set<AuditEventMetadataPayload>>(
        "metadata",
        FoundationJson,
        serializer<Set<AuditEventMetadataPayload>>()
    )
    public val message: Column<String?> = text("message").nullable()
}
