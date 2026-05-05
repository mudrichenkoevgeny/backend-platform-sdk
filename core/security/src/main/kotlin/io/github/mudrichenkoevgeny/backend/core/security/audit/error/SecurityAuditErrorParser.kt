package io.github.mudrichenkoevgeny.backend.core.security.audit.error

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.CommonAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.audit.metadata.SecurityAuditMetadataDeniedReasonValues
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser for security-related errors.
 *
 * Maps specific security failures (rate limits, weak passwords, MFA issues)
 * to a DENIED status with a corresponding reason code.
 */
@Singleton
class SecurityAuditErrorParser @Inject constructor() : AuditErrorParser {

    override fun parse(error: AppError): AuditErrorLogData? {
        val reason = when (error) {
            is CommonError.TooManyRequests -> SecurityAuditMetadataDeniedReasonValues.RATE_LIMIT
            is SecurityError.OtpRetryTooSoon -> SecurityAuditMetadataDeniedReasonValues.RATE_LIMIT
            is SecurityError.PasswordTooWeak -> SecurityAuditMetadataDeniedReasonValues.PASSWORD_TOO_WEAK
            is SecurityError.MfaTokenExpired -> SecurityAuditMetadataDeniedReasonValues.MFA_TOKEN_EXPIRED
            is SecurityError.TotpAlreadyEnabled -> SecurityAuditMetadataDeniedReasonValues.TOTP_ALREADY_ENABLED
            is SecurityError.TotpNotEnabled -> SecurityAuditMetadataDeniedReasonValues.TOTP_NOT_ENABLED
            is SecurityError.RecoveryCodeAlreadyUsed -> SecurityAuditMetadataDeniedReasonValues.RECOVERY_CODE_ALREADY_USED
            is SecurityError.TotpConfirmationRequired -> SecurityAuditMetadataDeniedReasonValues.MFA_REQUIRED
            else -> return null
        }

        return AuditErrorLogData(
            status = AuditStatus.DENIED,
            metadata = setOf(
                AuditEventMetadata(CommonAuditMetadataKey.ERROR_ID, error.errorId.asHexDashString()),
                AuditEventMetadata(CommonAuditMetadataKey.DENIED_REASON, reason)
            )
        )
    }
}