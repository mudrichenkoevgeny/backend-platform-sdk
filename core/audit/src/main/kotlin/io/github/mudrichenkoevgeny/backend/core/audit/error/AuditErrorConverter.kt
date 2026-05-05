package io.github.mudrichenkoevgeny.backend.core.audit.error

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the conversion of application errors into audit log data.
 *
 * Iterates through a set of specialized [AuditErrorParser]s to find a suitable match,
 * falling back to [CommonAuditErrorParser] if no specific parser can handle the error.
 */
@Singleton
class AuditErrorConverter @Inject constructor(
    private val parsers: Set<@JvmSuppressWildcards AuditErrorParser>,
    private val commonParser: CommonAuditErrorParser
) {
    /**
     * Converts the given [error] into [AuditErrorLogData] for audit logging purposes.
     */
    fun convert(error: AppError): AuditErrorLogData {
        val auditErrorLogData = parsers.firstNotNullOfOrNull { it.parse(error) }

        return auditErrorLogData ?: commonParser.parse(error)
    }
}