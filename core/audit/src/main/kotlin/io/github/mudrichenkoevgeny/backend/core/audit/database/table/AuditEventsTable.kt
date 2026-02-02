package io.github.mudrichenkoevgeny.backend.core.audit.database.table

import io.github.mudrichenkoevgeny.backend.core.audit.enums.AuditStatus
import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer
import org.jetbrains.exposed.v1.json.jsonb
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object AuditEventsTable : BaseTable("audit_events") {
    val actorId = uuid("actor_id").nullable()
    val action = varchar("action", BaseDbConstraints.DEFAULT_MAX_LENGTH)
    val resource = varchar("resource", BaseDbConstraints.DEFAULT_MAX_LENGTH)
    val resourceId = varchar("resource_id", BaseDbConstraints.DEFAULT_MAX_LENGTH).nullable()
    val status = enumerationByName("status", BaseDbConstraints.ENUM_MAX_LENGTH, AuditStatus::class)
    val metadata = jsonb<Map<String, JsonElement>>(
        "metadata",
        FoundationJson,
        serializer<Map<String, JsonElement>>()
    )
    val message = text("message").nullable()

    init {
        index("idx_audit_events_actor_id", isUnique = false, actorId)
        index("idx_audit_events_action", isUnique = false, action)
        index("idx_audit_events_resource_resource_id", isUnique = false, resource, resourceId)
        index("idx_audit_events_created_at", isUnique = false, createdAt)
    }
}