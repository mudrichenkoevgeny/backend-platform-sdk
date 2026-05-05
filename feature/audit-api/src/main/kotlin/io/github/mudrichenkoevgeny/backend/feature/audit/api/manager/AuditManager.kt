package io.github.mudrichenkoevgeny.backend.feature.audit.api.manager

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventId
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.listing.AuditSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.ListingParamNames
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

/**
 * Application-level API for creating and querying audit events.
 *
 * Use this interface to persist events via [createEvent] and to retrieve them
 * by id ([getEventById]) or with filters ([getEventsPage]).
 */
interface AuditManager {

    /**
     * Persists an [auditEvent] and returns the same event on success.
     */
    suspend fun createEvent(auditEvent: AuditEvent): AppResult<AuditEvent>

    /**
     * Returns the audit event with the given [eventId], or `null` if not found.
     */
    suspend fun getEventById(
        eventId: AuditEventId,
        userId: UserId,
        userPermissionCodes: Set<PermissionCode>
    ): AppResult<AuditEvent?>

    /**
     * Returns a paginated list of audit events matching the given filters.
     *
     * Pagination and sort follow [ListingParamNames]; filters match the management list API axes.
     *
     * @param pageParams One-based page and size ([ListingParamNames.Pagination]).
     * @param sortBy List `sort_by` ([ListingParamNames.Sort.SORT_BY]).
     * @param sortOrder List `sort_order` ([ListingParamNames.Sort.SORT_ORDER]).
     * @param actorIds Actor id filters.
     * @param actorTypes Actor type filters.
     * @param actorUserRoles Actor user role filters.
     * @param actions Action filters.
     * @param resources Resource type filters.
     * @param resourceIds Resource id filters.
     * @param statuses Status filters.
     * @param messages Message filters (case-insensitive substring).
     */
    suspend fun getEventsPage(
        managementUserPermissionCodes: Set<PermissionCode>,
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
        messages: List<String> = emptyList()
    ): AppResult<PagedResult<AuditEvent>>
}