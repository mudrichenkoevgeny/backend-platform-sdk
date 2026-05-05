package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.session

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetSessionsUseCase @Inject constructor(
    private val userManager: UserManager,
    private val sessionManager: SessionManager
) {
    /**
     * Retrieves a paginated and filtered list of active sessions for the current authenticated user.
     *
     * **Allowed Account Statuses:** Any.
     *
     * **Workflow:**
     * 1. Validates the existence and accessibility of the caller via [UserManager].
     * 2. Fetches a paged result of [UserSession] from [SessionManager] using provided filters and pagination params.
     * 3. Masks sensitive technical data in the result based on the user's session context.
     *
     * @param pageParams Pagination settings (index and size).
     * @param sortBy Field to sort the results by.
     * @param sortOrder Sorting direction ([SortOrder.ASC] or [SortOrder.DESC]).
     * @param identifiers Optional filter by identifier values.
     * @param identifierIds Optional filter by unique identifier IDs.
     * @param identifierAuthProviders Optional filter by [UserAuthProvider] types.
     * @param clientTypes Optional filter by [ClientType].
     * @param userAgents Optional filters for user-agent strings.
     * @param ipAddresses Optional filters for IP addresses.
     * @param languages Optional filters for client language headers.
     * @param deviceIds Optional filters for hardware device IDs.
     * @param deviceNames Optional filters for device names.
     * @param appVersions Optional filters for application versions.
     * @param operationSystemVersions Optional filters for OS versions.
     * @param authenticatedRequestContext The context of the authenticated request.
     * @return [AppResult] with [PagedResult] of [UserSession].
     */
    suspend operator fun invoke(
        pageParams: PageParams,
        sortBy: UserSortValues.UserSessionSortBy,
        sortOrder: SortOrder,
        identifiers: List<String>,
        identifierIds: List<UserIdentifierId>,
        identifierAuthProviders: List<UserAuthProvider>,
        clientTypes: List<ClientType>,
        userAgents: List<String>,
        ipAddresses: List<String>,
        languages: List<String>,
        deviceIds: List<String>,
        deviceNames: List<String>,
        appVersions: List<String>,
        operationSystemVersions: List<String>,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<PagedResult<UserSession>> {
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

        return sessionManager.getSessionsPageForSelf(
            userId = currentUserId,
            pageParams = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            identifiers = identifiers,
            identifierIds = identifierIds,
            identifierAuthProviders = identifierAuthProviders,
            clientTypes = clientTypes,
            userAgents = userAgents,
            ipAddresses = ipAddresses,
            languages = languages,
            deviceIds = deviceIds,
            deviceNames = deviceNames,
            appVersions = appVersions,
            operationSystemVersions = operationSystemVersions
        )
    }
}