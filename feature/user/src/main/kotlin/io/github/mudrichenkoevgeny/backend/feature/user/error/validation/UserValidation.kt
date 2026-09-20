package io.github.mudrichenkoevgeny.backend.feature.user.error.validation

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails

/**
 * Validates if the user has an allowed role and account status.
 * Returns an appropriate [UserError] if validation fails, or null if successful.
 */
fun UserDetails.validateRoleAndStatus(
    allowedRoles: Set<UserRole>,
    allowedAccountStatuses: Set<UserAccountStatus>
): AppError? {
    if (this.role !in allowedRoles) {
        return UserError.UserRoleNotAllowed(userId = this.id)
    }

    if (this.accountStatus !in allowedAccountStatuses) {
        return when (this.accountStatus) {
            UserAccountStatus.ACTIVE -> UserError.UserRoleNotAllowed(userId = this.id)
            UserAccountStatus.READ_ONLY -> UserError.UserReadOnly(userId = this.id)
            UserAccountStatus.BANNED -> UserError.UserBlocked(
                userId = this.id,
                blockedUntil = this.temporaryLockoutUntil
            )
            UserAccountStatus.SECURITY_HOLD -> UserError.UserSecurityHold(userId = this.id)
            UserAccountStatus.PENDING_DELETION -> UserError.UserPendingDeletion(userId = this.id)
        }
    }

    return null
}
