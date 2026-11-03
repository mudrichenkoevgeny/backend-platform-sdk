package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.auditevent

import io.github.mudrichenkoevgeny.backend.core.audit.network.model.ManagementAuditEventListQueryParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.AuditManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventSummary
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.permissions.AuditPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.navigation.screen.management.audit.list.ManagementAuditEventListParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAuditEventsUseCase @Inject constructor(
    private val userManager: UserManager,
    private val auditManager: AuditManager
) {
    /**
     * Retrieves a paginated and filtered list of audit events for management purposes.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY]
     * for the management caller.
     *
     * **Security:**
     * - Requires the management caller (STAFF or ADMIN) to be in an allowed status.
     * - [auditManager] applies permission-based filters: the caller only sees events for actor
     *   types (USER, SYSTEM, SERVICE, etc.) they are authorized to audit.
     * - Enforces data masking for actor IDs and sensitive metadata based on [AuditPermissionCode].
     *
     * **Workflow:**
     * 1. Validates the existence and status of the management caller.
     * 2. Delegates the filtered search to [auditManager], which reconciles the provided filters
     *    with the caller's permissions to ensure only authorized events are returned.
     *
     * @param queryParams Filter and pagination parameters.
     * @param authenticatedRequestContext The context of the authenticated management caller.
     * @return [AppResult] containing a [PagedResult] of [AuditEventSummary] within the authorized scope.
     */
    suspend operator fun invoke(
        queryParams: ManagementAuditEventListQueryParams,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<PagedResult<AuditEventSummary>> {
        val currentUserId = authenticatedRequestContext.userId

        val getCurrentUserResult = userManager.getUserInternalById(currentUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val currentUser = when (getCurrentUserResult) {
            is AppResult.Error -> return getCurrentUserResult
            is AppResult.Success -> getCurrentUserResult.data
        }

        return auditManager.getAuditEventsSummaryPage(
            managementUserPermissionCodes = currentUser.permissionCodes,
            queryParams = queryParams
        )
    }
}