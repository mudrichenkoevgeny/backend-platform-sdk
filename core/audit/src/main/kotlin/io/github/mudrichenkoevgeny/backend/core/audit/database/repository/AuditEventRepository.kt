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
     * - [actorId] — actor id ([AuditEvent.actorId]).
     * - [actorType] — [AuditEvent.actorType] (persisted wire / enum).
     * - [actorUserRole] — [AuditEvent.actorUserRole] (equality).
     * - [action] — [AuditEvent.action] (matches persisted [AuditActionType.serialName]).
     * - [resource] — [AuditEvent.resource] (matches persisted [AuditResourceType.serialName]).
     * - [resourceId] — [AuditEvent.resourceId] (equality).
     * - [status] — [AuditEvent.status].
     * - [message] — case-insensitive substring on [AuditEvent.message]; blank is treated as no filter.
     *
     * @param accessFilter defines the security boundaries by restricting the database query to specific
     * actor types and user roles.
     * @param pageParams One-based page and page size.
     * @param sortBy Sort field for the listing.
     * @param sortOrder Sort direction.
     * @param actorId Filter by actor id.
     * @param actorType Filter by actor type.
     * @param actorUserRole Filter by actor user role (equality).
     * @param action Filter by action type (persisted wire name).
     * @param resource Filter by resource type (persisted wire name).
     * @param resourceId Filter by resource instance id (equality).
     * @param status Filter by audit status.
     * @param message Optional substring filter on message.
     * @return [PagedResult] of matching events or an error.
     */
    suspend fun getEventsList(
        accessFilter: AuditAccessFilter,
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
        message: String? = null
    ): AppResult<PagedResult<AuditEvent>>
}