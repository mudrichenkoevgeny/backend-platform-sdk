package io.github.mudrichenkoevgeny.backend.core.audit.database.repository

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditAccessFilter
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

/**
 * Data access for audit events (persistence and querying).
 *
 * Implementations are responsible for storing events and returning paginated lists
 * with optional filters.
 */
interface AuditEventRepository {
    suspend fun createEvent(event: AuditEvent): AppResult<AuditEvent>

    suspend fun getEventById(auditEventId: AuditEventId): AppResult<AuditEvent?>

    /**
     * Returns a page of [AuditEvent] rows matching optional filters, ordered by [sortBy] and [sortOrder].
     *
     * **Pagination** (same semantics as [ListingParamNames.Pagination]): [PageParams.page] is the one-based
     * page index ([ListingParamNames.Pagination.PAGE_NUMBER]); [PageParams.size] is
     * [ListingParamNames.Pagination.PAGE_SIZE].
     *
     * **Sort** (same semantics as [ListingParamNames.Sort]): [sortBy] is the list `sort_by`
     * ([AuditSortValues.AuditEventSortBy.CREATED_AT] only); [sortOrder] is `sort_order` ([SortOrder] wire).
     *
     * **Filters** align with the management audit list API (audit event filter axis names / payload fields):
     * absent parameter means no filter on that axis. Non-null parameters combine as **AND**. Repeating the same
     * filter key as **OR** is not expressed here; compose that at a higher layer (e.g. HTTP handler) if needed.
     *
     * - [actorIds] — actor ids ([AuditEvent.actorId]).
     * - [actorTypes] — [AuditEvent.actorType] values.
     * - [actorUserRoles] — [AuditEvent.actorUserRole] values.
     * - [actions] — [AuditEvent.action] values (matches persisted [AuditActionType.serialName]).
     * - [resources] — [AuditEvent.resource] values (matches persisted [AuditResourceType.serialName]).
     * - [resourceIds] — [AuditEvent.resourceId] values.
     * - [statuses] — [AuditEvent.status] values.
     * - [messages] — case-insensitive substrings on [AuditEvent.message].
     *
     * @param accessFilter defines the security boundaries by restricting the database query to specific
     * actor types and user roles.
     * @param pageParams One-based page and page size.
     * @param sortBy Sort field for the listing.
     * @param sortOrder Sort direction.
     * @param actorIds Filters by actor ids.
     * @param actorTypes Filters by actor types.
     * @param actorUserRoles Filters by actor user roles.
     * @param actions Filters by action types.
     * @param resources Filters by resource types.
     * @param resourceIds Filters by resource instance ids.
     * @param statuses Filters by audit statuses.
     * @param messages Substring filters on message.
     * @return [PagedResult] of matching events or an error.
     */
    suspend fun getEventsPageWithAccessFilter(
        accessFilter: AuditAccessFilter,
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