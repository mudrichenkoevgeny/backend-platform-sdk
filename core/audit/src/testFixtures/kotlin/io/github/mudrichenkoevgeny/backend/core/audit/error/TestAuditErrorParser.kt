package io.github.mudrichenkoevgeny.backend.core.audit.error

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus

class TestAuditErrorParser(
    private val targetErrorCode: String = "special_error",
    private val resultStatus: AuditStatus = AuditStatus.DENIED
) : AuditErrorParser {
    override fun parse(error: AppError): AuditErrorLogData? {
        return if (error.code == targetErrorCode) {
            AuditErrorLogData(resultStatus, emptySet())
        } else null
    }
}
