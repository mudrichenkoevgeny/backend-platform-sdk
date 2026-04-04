package io.github.mudrichenkoevgeny.backend.core.audit.manager

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.mask.PayloadMaskingType
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
 * Application-level API for creating and querying audit events.
 *
 * Use this interface to persist events via [createEvent] and to retrieve them
 * by id ([getEventById]) or with filters ([getEventsList]).
 */
interface AuditManager {

    /**
     * Persists an [auditEvent] and returns the same event on success.
     */
    suspend fun createEvent(auditEvent: AuditEvent): AppResult<AuditEvent>

    /**
     * Returns the audit event with the given [eventId], or `null` if not found.
     *
     * @param payloadMaskingType When [PayloadMaskingType.MASKED], a non-null event is redacted like
     * [getEventsList] (metadata by sensitivity, selected `resourceId` kinds).
     */
    suspend fun getEventById(
        payloadMaskingType: PayloadMaskingType,
        eventId: AuditEventId
    ): AppResult<AuditEvent?>

    /**
     * Returns a paginated list of audit events matching the given filters.
     *
     * Pagination and sort follow [ListingParamNames]; filters match the management list API axes
     * (non-null parameters AND-combined; OR for repeated keys is handled outside this layer).
     *
     * @param pageParams One-based page and size ([ListingParamNames.Pagination]).
     * @param sortBy List `sort_by` ([ListingParamNames.Sort.SORT_BY]).
     * @param sortOrder List `sort_order` ([ListingParamNames.Sort.SORT_ORDER]).
     * @param actorId Actor id filter.
     * @param actorType Actor type filter.
     * @param actorUserRole Actor user role filter (equality).
     * @param action Action filter (persisted wire name).
     * @param resource Resource type filter (persisted wire name).
     * @param resourceId Resource id filter.
     * @param status Status filter.
     * @param message Case-insensitive substring on message when non-blank.
     * @param payloadMaskingType When [PayloadMaskingType.MASKED], list item metadata and sensitive
     * `resourceId` values (user email/phone resources) are redacted before return.
     */
    suspend fun getEventsList(
        payloadMaskingType: PayloadMaskingType,
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