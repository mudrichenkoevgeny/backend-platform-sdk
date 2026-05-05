package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagementGetIdentifiersUseCase @Inject constructor(
    private val userManager: UserManager,
    private val identifierManager: IdentifierManager
) {
    /**
     * Retrieves a paginated and filtered list of user identifiers for management purposes.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY]
     * for the management caller.
     *
     * **Security:**
     * - Requires the management caller (STAFF or ADMIN) to be in an allowed status.
     * - The [identifierManager] uses the caller's permission codes to filter visibility
     *   and apply masking to identifiers based on the owner's role (e.g., masked vs. unmasked emails).
     *
     * **Workflow:**
     * 1. Validates the existence and status of the management caller.
     * 2. Delegates the filtered search to [identifierManager], which enforces role-based
     *    access and data masking.
     *
     * @param pageParams pagination settings (page index and size).
     * @param sortBy field to sort by.
     * @param sortOrder sorting direction.
     * @param userIds optional filters for specific user IDs.
     * @param userAuthProviders optional filter by authentication provider types.
     * @param identifiers optional filter by identifier strings (e.g., specific email fragments).
     * @param authenticatedRequestContext context containing the ID of the management caller performing the search.
     * @return [AppResult] containing [PagedResult] of [UserIdentifier] within the caller's authorized scope.
     */
    suspend operator fun invoke(
        pageParams: PageParams,
        sortBy: UserSortValues.UserIdentifierSortBy,
        sortOrder: SortOrder,
        userIds: List<UserId>,
        userAuthProviders: List<UserAuthProvider>,
        identifiers: List<String>,
        authenticatedRequestContext: AuthenticatedRequestContext,
    ): AppResult<PagedResult<UserIdentifier>> {
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

        return identifierManager.getIdentifiersPageForManagement(
            managementUserPermissionCodes = managementUser.permissionCodes,
            pageParams = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            userIds = userIds,
            userAuthProviders = userAuthProviders,
            identifiers = identifiers
        )
    }
}
