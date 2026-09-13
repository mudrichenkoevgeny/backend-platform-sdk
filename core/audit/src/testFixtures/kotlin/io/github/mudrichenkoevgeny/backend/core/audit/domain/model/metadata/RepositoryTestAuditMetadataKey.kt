package io.github.mudrichenkoevgeny.backend.core.audit.domain.model.metadata

import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditValueSensitivity
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditMetadataKey

data class RepositoryTestAuditMetadataKey(
    override val serialName: String,
    override val valueSensitivity: AuditValueSensitivity = AuditValueSensitivity.NON_SENSITIVE
) : AuditMetadataKey {
    override fun parseOrNull(value: String): AuditMetadataKey = RepositoryTestAuditMetadataKey(value, valueSensitivity)
    override fun parseOrThrow(value: String): AuditMetadataKey = RepositoryTestAuditMetadataKey(value, valueSensitivity)
}

object AcceptAnyStringAuditMetadataKeyDelegate : AuditMetadataKey {
    override val serialName: String = "__test_metadata_key_delegate__"
    override val valueSensitivity: AuditValueSensitivity = AuditValueSensitivity.NON_SENSITIVE
    override fun parseOrNull(value: String): AuditMetadataKey = RepositoryTestAuditMetadataKey(value, valueSensitivity)
    override fun parseOrThrow(value: String): AuditMetadataKey = RepositoryTestAuditMetadataKey(value, valueSensitivity)
}
