package io.github.mudrichenkoevgeny.backend.core.audit.domain.model.action

import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType

data class RepositoryTestAuditAction(
    override val serialName: String
) : AuditActionType {
    override fun parseOrNull(value: String): AuditActionType = RepositoryTestAuditAction(value)
    override fun parseOrThrow(value: String): AuditActionType = RepositoryTestAuditAction(value)
}

object AcceptAnyStringAuditActionDelegate : AuditActionType {
    override val serialName: String = "__test_action_delegate__"
    override fun parseOrNull(value: String): AuditActionType = RepositoryTestAuditAction(value)
    override fun parseOrThrow(value: String): AuditActionType = RepositoryTestAuditAction(value)
}
