package com.mudrichenkoevgeny.backend.core.audit.model

import java.util.UUID

@JvmInline
value class AuditEventId(val value: UUID) {
    companion object {
        fun generate(): AuditEventId = AuditEventId(UUID.randomUUID())
    }
}