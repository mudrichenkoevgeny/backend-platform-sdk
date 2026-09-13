package io.github.mudrichenkoevgeny.backend.core.audit.domain.model.resource

import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType

data class RepositoryTestAuditResource(
    override val serialName: String
) : AuditResourceType {
    override fun parseOrNull(value: String): AuditResourceType = RepositoryTestAuditResource(value)
    override fun parseOrThrow(value: String): AuditResourceType = RepositoryTestAuditResource(value)
}

object AcceptAnyStringAuditResourceDelegate : AuditResourceType {
    override val serialName: String = "__test_resource_delegate__"
    override fun parseOrNull(value: String): AuditResourceType = RepositoryTestAuditResource(value)
    override fun parseOrThrow(value: String): AuditResourceType = RepositoryTestAuditResource(value)
}
