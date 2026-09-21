package io.github.mudrichenkoevgeny.backend.feature.user.error.validation

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import kotlin.time.Clock

/**
 * Validates if the user has an allowed role, account status, and is not locked out.
 * Returns [AppResult.Success] if validation passes, or [AppResult.Error] with an appropriate [UserError] if it fails.
 */
fun UserDetails.validateAccessEligibility(
    allowedRoles: Set<UserRole>,
    allowedAccountStatuses: Set<UserAccountStatus>
): AppResult<Unit> {
    if (this.role !in allowedRoles) {
        return AppResult.Error(UserError.UserRoleNotAllowed(userId = this.id))
    }

    if (this.accountStatus !in allowedAccountStatuses) {
        val error = when (this.accountStatus) {
            UserAccountStatus.ACTIVE -> UserError.UserRoleNotAllowed(userId = this.id)
            UserAccountStatus.READ_ONLY -> UserError.UserReadOnly(userId = this.id)
            UserAccountStatus.BANNED -> UserError.UserBanned(userId = this.id)
            UserAccountStatus.SECURITY_HOLD -> UserError.UserSecurityHold(userId = this.id)
            UserAccountStatus.PENDING_DELETION -> UserError.UserPendingDeletion(userId = this.id)
        }
        return AppResult.Error(error)
    }

    if (this.lockoutType == AccountLockoutType.INDEFINITE) {
        return AppResult.Error(
            UserError.UserLocked(
                userId = this.id,
                lockoutType = AccountLockoutType.INDEFINITE
            )
        )
    }

    if (this.lockoutType == AccountLockoutType.TEMPORARY) {
        val now = Clock.System.now()
        val temporaryLockoutUntil = this.temporaryLockoutUntil
        if (temporaryLockoutUntil == null || now < temporaryLockoutUntil) {
            return AppResult.Error(
                UserError.UserLocked(
                    userId = this.id,
                    lockoutType = AccountLockoutType.TEMPORARY,
                    temporaryLockoutUntil = temporaryLockoutUntil
                )
            )
        }
    }

    return AppResult.Success(Unit)
}
