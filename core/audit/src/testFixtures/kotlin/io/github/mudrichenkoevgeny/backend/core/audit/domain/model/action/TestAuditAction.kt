package io.github.mudrichenkoevgeny.backend.core.audit.domain.model.action

import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType

data class TestAuditAction(
    override val serialName: String = "test_action"
) : AuditActionType {
    override fun parseOrNull(value: String): AuditActionType? =
        if (value == serialName) this else null

    override fun parseOrThrow(value: String): AuditActionType =
        parseOrNull(value) ?: throw IllegalArgumentException("Invalid audit action: '$value'")
}
