package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetIdentifiersUseCase @Inject constructor(
    private val userManager: UserManager,
    private val identifierManager: IdentifierManager
) {
    /**
     * Retrieves a paginated list of authentication identifiers belonging to the current user.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY].
     *
     * **Workflow:**
     * 1. Validates the existence of the caller via [UserManager].
     * 2. Verifies that the account status permits data retrieval.
     * 3. Fetches a filtered and sorted page of identifiers via [IdentifierManager].
     *
     * @param authenticatedRequestContext Context containing the authenticated user's ID.
     * @param pageParams Pagination settings (page index and size).
     * @param sortBy Field to sort by (e.g., created at, updated at).
     * @param sortOrder Sorting direction (ASC/DESC).
     * @param userAuthProviders Optional filter by authentication provider types (EMAIL, PHONE, etc.).
     * @param identifiers Optional filter by specific identifier substrings.
     * @return [AppResult] with [PagedResult] of [UserIdentifier].
     */
    suspend operator fun invoke(
        authenticatedRequestContext: AuthenticatedRequestContext,
        pageParams: PageParams,
        sortBy: UserSortValues.UserIdentifierSortBy,
        sortOrder: SortOrder,
        userAuthProviders: List<UserAuthProvider>,
        identifiers: List<String>
    ): AppResult<PagedResult<UserIdentifier>> {
        val currentUserId = authenticatedRequestContext.userId

        val getCurrentUserResult = userManager.getUserByIdForSelf(currentUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val currentUser = when (getCurrentUserResult) {
            is AppResult.Error -> return getCurrentUserResult
            is AppResult.Success -> getCurrentUserResult.data
        }

        if (currentUser.accountStatus !in setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)) {
            return AppResult.Error(UserError.UserForbidden(currentUserId))
        }

        return identifierManager.getIdentifiersPageForSelf(
            userId = currentUserId,
            pageParams = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            userAuthProviders = userAuthProviders,
            identifiers = identifiers
        )
    }
}