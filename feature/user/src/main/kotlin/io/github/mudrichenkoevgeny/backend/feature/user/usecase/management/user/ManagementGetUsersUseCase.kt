package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.common.result.mapSuccess
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.UserListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserSummary
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.user.toUserSummary
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
     * @param queryParams Filtered search parameters.
     * @param authenticatedRequestContext Context containing the ID of the manager performing the search.
     * @return [AppResult] containing [PagedResult] of [UserSummary].
     */
    suspend operator fun invoke(
        queryParams: UserListQueryParams,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<PagedResult<UserSummary>> {
        val managementUserId = authenticatedRequestContext.userId

        val getManagementUserResult = userManager.getUserInternalById(managementUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val managementUser = when (getManagementUserResult) {
            is AppResult.Error -> return getManagementUserResult
            is AppResult.Success -> getManagementUserResult.data
        }

        if (managementUser.accountStatus != UserAccountStatus.ACTIVE) {
            return AppResult.Error(UserError.UserIllegalAccountStatus(managementUserId))
        }

        return userManager.getUsersInternalPage(
            managementUserPermissionCodes = managementUser.permissionCodes,
            queryParams = queryParams
        ).mapSuccess { paged ->
            PagedResult(
                items = paged.items.map { it.toUserSummary() },
                totalCount = paged.totalCount,
                pageNumber = paged.pageNumber,
                pageSize = paged.pageSize,
                totalPages = paged.totalPages
            )
        }
    }
}
