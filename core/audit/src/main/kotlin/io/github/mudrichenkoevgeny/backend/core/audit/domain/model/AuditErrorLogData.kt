package io.github.mudrichenkoevgeny.backend.core.audit.domain.model

import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus

/**
 * Represents error-specific data extracted for an audit record.
 *
 * Used to enrich audit events with relevant [AuditStatus] and metadata when an operation fails.
 */
data class AuditErrorLogData(
    val status: AuditStatus,
    val metadata: Set<AuditEventMetadata>
)
