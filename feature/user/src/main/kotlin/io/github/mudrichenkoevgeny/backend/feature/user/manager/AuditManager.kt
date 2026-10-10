package io.github.mudrichenkoevgeny.backend.feature.user.manager

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventId
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventInternal
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventPrivate
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventSummary
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
 * Use this interface to persist internal events via [createEvent] and to retrieve them
 * by id ([getAuditEventPrivateById]) or with filters as summaries ([getAuditEventsSummaryPage]).
 */
interface AuditManager {

    /**
     * Persists an [auditEvent] and returns the internal event representation on success.
     *
     * @param auditEvent internal event data
     * @return [AppResult.Success] containing created [AuditEventInternal] or an error
     */
    suspend fun createEvent(auditEvent: AuditEventInternal): AppResult<AuditEventInternal>

    /**
     * Returns the detailed private audit event with the given [eventId], or `null` if not found or unauthorized.
     *
     * @param eventId event unique identifier
     * @param userId caller user id
     * @param userPermissionCodes caller permissions used to evaluate visibility and unmasking
     * @return [AppResult.Success] containing [AuditEventPrivate] if found and allowed, `null` if missing, or an error
     */
    suspend fun getAuditEventPrivateById(
        eventId: AuditEventId,
        userId: UserId,
        userPermissionCodes: Set<PermissionCode>
    ): AppResult<AuditEventPrivate?>

    /**
     * Returns a paginated list of condensed audit event summaries matching the given filters.
     *
     * Pagination and sort follow [ListingParamNames]; filters match the management list API axes.
     *
     * @param managementUserPermissionCodes caller permissions used to evaluate access rules and masking
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
     * @return [AppResult.Success] containing [PagedResult] of [AuditEventSummary] or an error
     */
    suspend fun getAuditEventsSummaryPage(
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
    ): AppResult<PagedResult<AuditEventSummary>>
}
