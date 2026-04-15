package io.github.mudrichenkoevgeny.backend.core.security.audit

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.CommonAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.audit.metadata.SecurityAuditMetadataDeniedReasonValues

fun AppError.toDeniedSecurityAuditEventMetadata(): Set<AuditEventMetadata> {
    val deniedReasonValue = when (this) {
        is CommonError.TooManyRequests -> SecurityAuditMetadataDeniedReasonValues.RATE_LIMIT
        else -> SecurityAuditMetadataDeniedReasonValues.RATE_LIMIT
    }
    return setOf(AuditEventMetadata(CommonAuditMetadataKey.DENIED_REASON, deniedReasonValue))
}