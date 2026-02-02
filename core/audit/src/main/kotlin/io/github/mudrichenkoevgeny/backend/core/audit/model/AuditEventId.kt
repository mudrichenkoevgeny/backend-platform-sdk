package io.github.mudrichenkoevgeny.backend.core.audit.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@JvmInline
value class AuditEventId(val value: Uuid) {
    fun asHexDashString(): String = value.toHexDashString()

    companion object {
        fun generate() = AuditEventId(Uuid.random())
    }
}