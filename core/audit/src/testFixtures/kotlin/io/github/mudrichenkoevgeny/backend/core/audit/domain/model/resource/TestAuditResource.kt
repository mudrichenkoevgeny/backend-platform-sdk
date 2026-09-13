package io.github.mudrichenkoevgeny.backend.core.audit.domain.model.resource

import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType

data class TestAuditResource(
    override val serialName: String = "test_resource"
) : AuditResourceType {
    override fun parseOrNull(value: String): AuditResourceType? =
        if (value == serialName) this else null

    override fun parseOrThrow(value: String): AuditResourceType =
        parseOrNull(value) ?: throw IllegalArgumentException("Invalid audit resource: '$value'")
}
