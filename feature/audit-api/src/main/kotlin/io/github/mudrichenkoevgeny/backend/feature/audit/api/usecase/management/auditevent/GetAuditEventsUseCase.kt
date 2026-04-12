package io.github.mudrichenkoevgeny.backend.feature.audit.api.usecase.management.auditevent

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.audit.api.manager.AuditManager
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.listing.AuditSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAuditEventsUseCase @Inject constructor(
    private val userManager: UserManager,
    private val auditManager: AuditManager
) {
    suspend operator fun invoke(
        pageParams: PageParams,
        sortBy: AuditSortValues.AuditEventSortBy = AuditSortValues.AuditEventSortBy.CREATED_AT,
        sortOrder: SortOrder = SortOrder.DESC,
        actorId: String? = null,
        actorType: AuditActorType? = null,
        actorUserRole: String? = null,
        action: AuditActionType? = null,
        resource: AuditResourceType? = null,
        resourceId: String? = null,
        status: AuditStatus? = null,
        message: String? = null,
        requestContext: RequestContext
    ): AppResult<PagedResult<AuditEvent>> {
        val currentUserId = requestContext.userId
            ?: return AppResult.Error(UserError.UserForbidden())

        val getCurrentUserResult = userManager.getUserById(currentUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val currentUser = when (getCurrentUserResult) {
            is AppResult.Error -> return getCurrentUserResult
            is AppResult.Success -> getCurrentUserResult.data
        }

        return auditManager.getEventsList(
            userPermissionCodes = currentUser.permissions,
            pageParams = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            actorId = actorId,
            actorType = actorType,
            actorUserRole = actorUserRole,
            action = action,
            resource = resource,
            resourceId = resourceId,
            status = status,
            message = message
        )
    }
}
