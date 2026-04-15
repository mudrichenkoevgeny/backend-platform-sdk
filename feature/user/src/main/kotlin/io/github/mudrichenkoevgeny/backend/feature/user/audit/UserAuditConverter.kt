package io.github.mudrichenkoevgeny.backend.feature.user.audit

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.CommonAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.metadata.UserAuditMetadataDeniedReasonValues

fun AppError.toDeniedUserAuditEventMetadata(): Set<AuditEventMetadata> {
   val deniedReasonValue = when (this) {
       is UserError.UserForbidden -> UserAuditMetadataDeniedReasonValues.USER_FORBIDDEN
       is UserError.UserReadOnly -> UserAuditMetadataDeniedReasonValues.USER_READ_ONLY
       is UserError.UserBlocked -> UserAuditMetadataDeniedReasonValues.USER_BLOCKED
       is UserError.UserSecurityHold -> UserAuditMetadataDeniedReasonValues.USER_SECURITY_HOLD
       is UserError.UserPendingDeletion -> UserAuditMetadataDeniedReasonValues.USER_PENDING_DELETION
       is UserError.UserNotFound -> UserAuditMetadataDeniedReasonValues.USER_NOT_FOUND
       is UserError.UserRoleNotAllowed -> UserAuditMetadataDeniedReasonValues.USER_ROLE_NOT_ALLOWED
       is UserError.UserMissingPermissions -> UserAuditMetadataDeniedReasonValues.USER_MISSING_PERMISSIONS
       else -> UserAuditMetadataDeniedReasonValues.USER_FORBIDDEN
    }
    return setOf(AuditEventMetadata(CommonAuditMetadataKey.DENIED_REASON, deniedReasonValue))
}

fun AppError.toErrorUserAuditEventMetadata(): Set<AuditEventMetadata> {
    return setOf(
        AuditEventMetadata(CommonAuditMetadataKey.ERROR_ID, this.errorId.asHexDashString()),
        AuditEventMetadata(CommonAuditMetadataKey.ERROR_CODE, this.code)
    )
}