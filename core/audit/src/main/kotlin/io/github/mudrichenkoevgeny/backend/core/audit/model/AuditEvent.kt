package io.github.mudrichenkoevgeny.backend.core.audit.model

import io.github.mudrichenkoevgeny.backend.core.audit.enums.AuditStatus
import kotlinx.serialization.json.JsonElement
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
data class AuditEvent(
    val id: AuditEventId = AuditEventId.generate(),
    val actorId: Uuid? = null,
    val action: String,
    val resource: String,
    val resourceId: String? = null,
    val status: AuditStatus,
    val metadata: Map<String, JsonElement> = emptyMap(),
    val message: String? = null
)