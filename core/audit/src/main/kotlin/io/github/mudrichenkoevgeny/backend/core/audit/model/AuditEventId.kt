package io.github.mudrichenkoevgeny.backend.core.audit.model

import kotlin.uuid.Uuid

@JvmInline
value class AuditEventId(val value: Uuid) {
    fun asHexDashString(): String = value.toHexDashString()

    companion object {
        fun generate() = AuditEventId(Uuid.random())
    }
}