package io.github.mudrichenkoevgeny.backend.core.audit.error

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError

/**
 * Parser that extracts audit-specific information from application errors.
 */
interface AuditErrorParser {
    /**
     * Attempts to map an [AppError] to [AuditErrorLogData].
     * Returns null if the error is not handled by this parser.
     */
    fun parse(error: AppError): AuditErrorLogData?
}