package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.UserPermissionCode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagementGetUserUseCase @Inject constructor(
    private val userManager: UserManager
) {
    /**
     * Retrieves [UserDetails] for a specific user within the management scope.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY]
     * for the management caller.
     *
     * **Security:**
     * - Verifies that the management caller (STAFF or ADMIN) is active.
     * - The [userManager] verifies if the caller has the required [UserPermissionCode]
     *   to view the specific target user based on the target's role.
     *
     * **Workflow:**
     * 1. Validates the management caller's status.
     * 2. Fetches the target [userId] via [userManager] with a permission check.
     *
     * @param userId The ID of the target user to retrieve.
     * @param authenticatedRequestContext The context of the authenticated management request.
     * @return [AppResult] containing [UserDetails] if found and authorized.
     */
    suspend operator fun invoke(
        userId: UserId,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<UserDetails> {
        val managementUserId = authenticatedRequestContext.userId

        val getManagementUserResult = userManager.getUserByIdForSelf(managementUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val managementUser = when (getManagementUserResult) {
            is AppResult.Error -> return getManagementUserResult
            is AppResult.Success -> getManagementUserResult.data
        }

        if (managementUser.accountStatus != UserAccountStatus.ACTIVE) {
            return AppResult.Error(UserError.UserIllegalAccountStatus(managementUserId))
        }

        return userManager.getUserForManagement(
            userId = userId,
            managementUserId = managementUserId,
            managementUserPermissionCodes = managementUser.permissionCodes
        ).mapNotNullOrError(UserError.UserNotFound(userId))
    }
}