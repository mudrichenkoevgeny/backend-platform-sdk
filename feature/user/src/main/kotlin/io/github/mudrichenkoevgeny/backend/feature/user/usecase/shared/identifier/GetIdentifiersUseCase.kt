package io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.identifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.SelfIdentifierListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierSummary
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to retrieve a paginated list of user identifiers.
 */
@Singleton
public class GetIdentifiersUseCase @Inject constructor(
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
     * @param queryParams Filtered search parameters.
     * @param authenticatedRequestContext Context containing the authenticated user's ID.
     * @return [AppResult] with [PagedResult] of [UserIdentifierSummary].
     */
    public suspend operator fun invoke(
        queryParams: SelfIdentifierListQueryParams,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<PagedResult<UserIdentifierSummary>> {
        val currentUserId = authenticatedRequestContext.userId

        val getCurrentUserResult = userManager.getUserInternalById(currentUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val currentUser = when (getCurrentUserResult) {
            is AppResult.Error -> return getCurrentUserResult
            is AppResult.Success -> getCurrentUserResult.data
        }

        if (currentUser.accountStatus !in setOf(
                UserAccountStatus.ACTIVE,
                UserAccountStatus.READ_ONLY
            )
        ) {
            return AppResult.Error(UserError.UserForbidden(currentUserId))
        }

        return identifierManager.getUserIdentifiersSummaryPage(
            userId = currentUserId,
            queryParams = queryParams
        )
    }
}
