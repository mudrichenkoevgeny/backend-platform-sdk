package io.github.mudrichenkoevgeny.backend.core.audit.error

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.CommonAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fallback parser that maps any [AppError] to a generic FAILED status with error details.
 */
@Singleton
class CommonAuditErrorParser @Inject constructor() : AuditErrorParser {

    override fun parse(error: AppError): AuditErrorLogData {
        return AuditErrorLogData(
            status = AuditStatus.FAILED,
            metadata = setOf(
                AuditEventMetadata(CommonAuditMetadataKey.ERROR_ID, error.errorId.asHexDashString()),
                AuditEventMetadata(CommonAuditMetadataKey.ERROR_CODE, error.code)
            )
        )
    }
}