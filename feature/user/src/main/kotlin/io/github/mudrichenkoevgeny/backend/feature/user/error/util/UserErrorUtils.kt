package io.github.mudrichenkoevgeny.backend.feature.user.error.util

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

/**
 * Attempts to extract the associated [UserId] from an [AppError] if it is a [UserError] containing a user ID.
 *
 * @return The [UserId] associated with the error, or null if not applicable or missing.
 */
fun AppError.extractUserIdOrNull(): UserId? {
    val userError = this as? UserError ?: return null
    return when (userError) {
        is UserError.UserBanned -> userError.userId
        is UserError.UserLocked -> userError.userId
        is UserError.UserReadOnly -> userError.userId
        is UserError.UserSecurityHold -> userError.userId
        is UserError.UserPendingDeletion -> userError.userId
        is UserError.UserForbidden -> userError.userId
        is UserError.UserRoleNotAllowed -> userError.userId
        is UserError.UserMissingPermissions -> userError.userId
        is UserError.UserIllegalAccountStatus -> userError.userId
        is UserError.UserInsufficientAuthorityLevel -> userError.userId
        is UserError.UserNotFound -> userError.userId
        is UserError.PasswordSetupRequired -> userError.userId
        else -> null
    }
}

/**
 * Attempts to extract the associated [UserId] UUID hex-with-dashes string from an [AppError] if it is a [UserError] containing a user ID.
 *
 * @return The UUID hex-with-dashes string associated with the error, or null if not applicable or missing.
 */
fun AppError.extractUserIdHexOrNull(): String? = extractUserIdOrNull()?.asHexDashString()
