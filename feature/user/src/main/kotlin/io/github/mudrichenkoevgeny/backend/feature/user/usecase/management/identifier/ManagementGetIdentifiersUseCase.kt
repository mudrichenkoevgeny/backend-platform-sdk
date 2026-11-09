package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.ManagementIdentifierListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierSummary
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to get identifiers.
 */
@Singleton
public class ManagementGetIdentifiersUseCase @Inject constructor(
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
     * @param queryParams Filtered search parameters.
     * @param authenticatedRequestContext Context containing the ID of the management caller performing the search.
     * @return [AppResult] containing [PagedResult] of [UserIdentifierSummary] within the caller's authorized scope.
     */
    public suspend operator fun invoke(
        queryParams: ManagementIdentifierListQueryParams,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<PagedResult<UserIdentifierSummary>> {
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

        return identifierManager.getUserIdentifiersSummaryPage(
            managementUserPermissionCodes = managementUser.permissionCodes,
            queryParams = queryParams
        )
    }
}
