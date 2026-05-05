package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagementGetUsersUseCase @Inject constructor(
    private val userManager: UserManager
) {
    /**
     * Retrieves a paginated and filtered list of users for management purposes.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY]
     * for the management caller.
     *
     * **Security:**
     * - Requires the caller to be an active member of the **management staff (STAFF or ADMIN)**.
     * - Uses the **caller's** permission codes to filter the result set, ensuring they only see
     *   users they are authorized to view.
     *
     * **Workflow:**
     * 1. Validates the existence and status of the management caller.
     * 2. Delegates the filtered search to [userManager] which applies role-based visibility rules.
     *
     * @param pageParams pagination settings (page index and size).
     * @param sortBy field to sort by.
     * @param sortOrder sorting direction.
     * @param roles filter by [UserDetails.role].
     * @param accountStatuses filter by [UserDetails.accountStatus].
     * @param accountStatusesBeforeDeletion filter by [UserDetails.accountStatusBeforeDeletion].
     * @param authorityLevelFrom inclusive lower bound for [UserDetails.authorityLevel].
     * @param authorityLevelTo inclusive upper bound for [UserDetails.authorityLevel].
     * @param requiredPermissionCodes filter for users possessing all of these permission codes.
     * @param isTotpEnabled filter by MFA status.
     * @param authenticatedRequestContext context containing the ID of the manager performing the search.
     * @return [AppResult] containing [PagedResult] of [UserDetails].
     */
    suspend operator fun invoke(
        pageParams: PageParams,
        sortBy: UserSortValues.UserSortBy,
        sortOrder: SortOrder,
        roles: List<UserRole>,
        accountStatuses: List<UserAccountStatus>,
        accountStatusesBeforeDeletion: List<UserAccountStatus>,
        authorityLevelFrom: Int?,
        authorityLevelTo: Int?,
        requiredPermissionCodes: Set<PermissionCode>,
        isTotpEnabled: Boolean?,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<PagedResult<UserDetails>> {
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

        return userManager.getUsersPageForManagement(
            managementUserPermissionCodes = managementUser.permissionCodes,
            pageParams = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            roles = roles,
            accountStatuses = accountStatuses,
            accountStatusesBeforeDeletion = accountStatusesBeforeDeletion,
            authorityLevelFrom = authorityLevelFrom,
            authorityLevelTo = authorityLevelTo,
            permissionCodes = requiredPermissionCodes,
            isTotpEnabled = isTotpEnabled
        )
    }
}