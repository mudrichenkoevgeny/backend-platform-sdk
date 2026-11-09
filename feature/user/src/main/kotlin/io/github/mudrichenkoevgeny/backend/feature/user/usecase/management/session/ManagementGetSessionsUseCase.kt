package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.session

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.ManagementSessionListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionSummary
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to get sessions.
 */
@Singleton
public class ManagementGetSessionsUseCase @Inject constructor(
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
     * @param queryParams Filtered search parameters.
     * @param authenticatedRequestContext Context containing the ID of the management caller performing the search.
     * @return [AppResult] containing [PagedResult] of [UserSessionSummary] within the caller's authorized scope.
     */
    public suspend operator fun invoke(
        queryParams: ManagementSessionListQueryParams,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<PagedResult<UserSessionSummary>> {
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

        return sessionManager.getUserSessionsSummaryPage(
            managementUserPermissionCodes = managementUser.permissionCodes,
            queryParams = queryParams
        )
    }
}
