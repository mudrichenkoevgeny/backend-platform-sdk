package io.github.mudrichenkoevgeny.backend.feature.auditapi.usecase.management.auditevent

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.auditapi.api.manager.AuditManager
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.listing.AuditSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.auditapi.domain.permissions.AuditPermissionCode
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
     * @param pageParams Pagination settings (index and size).
     * @param sortBy Field to sort by (defaults to CREATED_AT).
     * @param sortOrder Sorting direction (defaults to DESC).
     * @param actorIds Filter by specific actor identifiers.
     * @param actorTypes Filter by types of actors (e.g., USER, SYSTEM).
     * @param actorUserRoles Filter by specific user roles of the actors.
     * @param actions Filter by specific audit actions.
     * @param resources Filter by targeted resource types.
     * @param resourceIds Filter by specific resource identifiers.
     * @param statuses Filter by audit event statuses (e.g., SUCCESS, FAILURE).
     * @param messages Filter by free-text audit messages.
     * @param authenticatedRequestContext The context of the authenticated management caller.
     * @return [AppResult] containing a [PagedResult] of [AuditEvent] within the authorized scope.
     */
    suspend operator fun invoke(
        pageParams: PageParams,
        sortBy: AuditSortValues.AuditEventSortBy = AuditSortValues.AuditEventSortBy.CREATED_AT,
        sortOrder: SortOrder = SortOrder.DESC,
        actorIds: List<String> = emptyList(),
        actorTypes: List<AuditActorType> = emptyList(),
        actorUserRoles: List<String> = emptyList(),
        actions: List<AuditActionType> = emptyList(),
        resources: List<AuditResourceType> = emptyList(),
        resourceIds: List<String> = emptyList(),
        statuses: List<AuditStatus> = emptyList(),
        messages: List<String> = emptyList(),
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<PagedResult<AuditEvent>> {
        val currentUserId = authenticatedRequestContext.userId

        val getCurrentUserResult = userManager.getUserByIdForSelf(currentUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val currentUser = when (getCurrentUserResult) {
            is AppResult.Error -> return getCurrentUserResult
            is AppResult.Success -> getCurrentUserResult.data
        }

        return auditManager.getEventsPage(
            managementUserPermissionCodes = currentUser.permissionCodes,
            pageParams = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            actorIds = actorIds,
            actorTypes = actorTypes,
            actorUserRoles = actorUserRoles,
            actions = actions,
            resources = resources,
            resourceIds = resourceIds,
            statuses = statuses,
            messages = messages
        )
    }
}