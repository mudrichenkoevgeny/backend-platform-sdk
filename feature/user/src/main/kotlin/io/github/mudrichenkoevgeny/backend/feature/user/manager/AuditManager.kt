package io.github.mudrichenkoevgeny.backend.feature.user.manager

import io.github.mudrichenkoevgeny.backend.core.audit.network.model.ManagementAuditEventListQueryParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventId
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventInternal
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventPrivate
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventSummary
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

/**
 * Application-level API for creating and querying audit events.
 *
 * Use this interface to persist internal events via [createEvent] and to retrieve them
 * by id ([getAuditEventPrivateById]) or with filters as summaries ([getAuditEventsSummaryPage]).
 */
public interface AuditManager {
    /**
     * Persists an [auditEvent] and returns the internal event representation on success.
     *
     * @param auditEvent internal event data
     * @return [AppResult.Success] containing created [AuditEventInternal] or an error
     */
    public suspend fun createEvent(auditEvent: AuditEventInternal): AppResult<AuditEventInternal>

    /**
     * Returns the detailed private audit event with the given [eventId], or `null` if not found or unauthorized.
     *
     * @param eventId event unique identifier
     * @param userId caller user id
     * @param userPermissionCodes caller permissions used to evaluate visibility and unmasking
     * @return [AppResult.Success] containing [AuditEventPrivate] if found and allowed, `null` if missing, or an error
     */
    public suspend fun getAuditEventPrivateById(
        eventId: AuditEventId,
        userId: UserId,
        userPermissionCodes: Set<PermissionCode>
    ): AppResult<AuditEventPrivate?>

    /**
     * Returns a paginated list of condensed audit event summaries matching the given filters in [queryParams].
     *
     * @param managementUserPermissionCodes caller permissions used to evaluate access rules and masking
     * @param queryParams query parameters and filters
     * @return [AppResult.Success] containing [PagedResult] of [AuditEventSummary] or an error
     */
    public suspend fun getAuditEventsSummaryPage(
        managementUserPermissionCodes: Set<PermissionCode>,
        queryParams: ManagementAuditEventListQueryParams
    ): AppResult<PagedResult<AuditEventSummary>>
}
