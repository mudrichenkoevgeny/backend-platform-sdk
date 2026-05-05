package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.session

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
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSession
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagementGetSessionsUseCase @Inject constructor(
    private val userManager: UserManager,
    private val sessionManager: SessionManager
) {
    /**
     * Retrieves a paginated and filtered list of user sessions for management purposes.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY]
     * for the management caller.
     *
     * **Security:**
     * - Requires the management caller (STAFF or ADMIN) to be active.
     * - The [sessionManager] uses the caller's permission codes to filter visibility and apply
     *   masking to sensitive session data based on the target session owner's role.
     *
     * **Workflow:**
     * 1. Validates the existence and status of the management caller.
     * 2. Delegates the filtered search to [sessionManager], which enforces role-based
     *    access and data masking (e.g., masked vs. unmasked session details).
     *
     * @param pageParams pagination settings (page index and size).
     * @param sortBy field to sort by.
     * @param sortOrder sorting direction.
     * @param userIds optional filters for specific user IDs.
     * @param userRoles optional filters for specific user roles.
     * @param identifiers optional filters for identifier values (e.g., emails).
     * @param identifierIds optional filters for specific user identifier IDs.
     * @param identifierAuthProviders optional filter by authentication provider types.
     * @param clientTypes optional filter by client platform types.
     * @param userAgents optional filters for client user-agent strings.
     * @param ipAddresses optional filters for client IP addresses.
     * @param languages optional filters for client language headers.
     * @param deviceIds optional filters for hardware device IDs.
     * @param deviceNames optional filters for human-readable device names.
     * @param appVersions optional filters for application versions.
     * @param operationSystemVersions optional filters for OS versions.
     * @param authenticatedRequestContext context containing the ID of the management caller performing the search.
     * @return [AppResult] containing [PagedResult] of [UserSession] within the caller's authorized scope.
     */
    suspend operator fun invoke(
        pageParams: PageParams,
        sortBy: UserSortValues.UserSessionSortBy,
        sortOrder: SortOrder,
        userIds: List<UserId>,
        userRoles: List<UserRole>,
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

        return sessionManager.getSessionsPageForManagement(
            managementUserPermissionCodes = managementUser.permissionCodes,
            pageParams = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            userIds = userIds,
            userRoles = userRoles,
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
