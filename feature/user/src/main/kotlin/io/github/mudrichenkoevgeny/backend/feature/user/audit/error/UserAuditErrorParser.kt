package io.github.mudrichenkoevgeny.backend.feature.user.audit.error

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.CommonAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.metadata.UserAuditMetadataDeniedReasonValues
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser for user-domain errors.
 *
 * Maps user-specific access violations (blocked status, missing permissions,
 * role mismatch) to a DENIED status.
 */
@Singleton
class UserAuditErrorParser @Inject constructor() : AuditErrorParser {

    override fun parse(error: AppError): AuditErrorLogData? {
        if (error !is UserError) return null

        val deniedReasonValue = when (error) {
            is UserError.UserForbidden -> UserAuditMetadataDeniedReasonValues.USER_FORBIDDEN
            is UserError.UserReadOnly -> UserAuditMetadataDeniedReasonValues.USER_READ_ONLY
            is UserError.UserBlocked -> UserAuditMetadataDeniedReasonValues.USER_BLOCKED
            is UserError.UserSecurityHold -> UserAuditMetadataDeniedReasonValues.USER_SECURITY_HOLD
            is UserError.UserPendingDeletion -> UserAuditMetadataDeniedReasonValues.USER_PENDING_DELETION
            is UserError.UserNotFound -> UserAuditMetadataDeniedReasonValues.USER_NOT_FOUND
            is UserError.UserRoleNotAllowed -> UserAuditMetadataDeniedReasonValues.USER_ROLE_NOT_ALLOWED
            is UserError.UserMissingPermissions -> UserAuditMetadataDeniedReasonValues.USER_MISSING_PERMISSIONS
            is UserError.UserInsufficientAuthorityLevel -> UserAuditMetadataDeniedReasonValues.USER_INSUFFICIENT_AUTHORITY_LEVEL
            is UserError.UserIllegalAccountStatus -> UserAuditMetadataDeniedReasonValues.USER_ILLEGAL_ACCOUNT_STATUS
            is UserError.UserIdentifierLimitReached -> UserAuditMetadataDeniedReasonValues.USER_IDENTIFIER_LIMIT_REACHED
            is UserError.TotalUserIdentifiersLimitReached -> UserAuditMetadataDeniedReasonValues.TOTAL_USER_IDENTIFIERS_LIMIT_REACHED
            else -> return null
        }

        return AuditErrorLogData(
            status = AuditStatus.DENIED,
            metadata = setOf(
                AuditEventMetadata(CommonAuditMetadataKey.ERROR_ID, error.errorId.asHexDashString()),
                AuditEventMetadata(CommonAuditMetadataKey.DENIED_REASON, deniedReasonValue)
            )
        )
    }
}