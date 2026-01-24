package com.mudrichenkoevgeny.backend.core.audit.model

import com.mudrichenkoevgeny.backend.core.audit.enums.AuditStatus
import kotlinx.serialization.json.JsonElement
import java.util.UUID

data class AuditEvent(
    val id: AuditEventId = AuditEventId.generate(),
    val actorId: UUID? = null,
    val action: String,
    val resource: String,
    val resourceId: String? = null,
    val status: AuditStatus,
    val metadata: Map<String, JsonElement> = emptyMap(),
    val message: String? = null
)