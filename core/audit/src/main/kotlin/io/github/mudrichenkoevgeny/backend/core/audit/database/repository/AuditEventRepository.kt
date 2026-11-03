package io.github.mudrichenkoevgeny.backend.core.audit.database.repository

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditAccessFilter
import io.github.mudrichenkoevgeny.backend.core.audit.network.model.ManagementAuditEventListQueryParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventId
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventInternal
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult

/**
 * Data access for audit events (persistence and querying).
 *
 * Implementations are responsible for storing events and returning paginated lists
 * with optional filters.
 */
interface AuditEventRepository {
    /**
     * Persists a new [AuditEventInternal] in the database.
     *
     * @param event The audit event data to insert.
     * @return [AppResult] containing the inserted [AuditEventInternal] or an error.
     */
    suspend fun createEvent(event: AuditEventInternal): AppResult<AuditEventInternal>

    /**
     * Retrieves an [AuditEventInternal] by its unique [AuditEventId].
     *
     * @param auditEventId The unique identifier of the audit event.
     * @return [AppResult] containing the [AuditEventInternal] if found, `null` if not found, or an error.
     */
    suspend fun getEventById(auditEventId: AuditEventId): AppResult<AuditEventInternal?>

    /**
     * Returns a page of [AuditEventInternal] rows matching optional filters in [queryParams].
     *
     * @param accessFilter defines the security boundaries by restricting the database query to specific
     * actor types and user roles.
     * @param queryParams query parameters and filters
     * @return [PagedResult] of matching events or an error.
     */
    suspend fun getEventsPageWithAccessFilter(
        accessFilter: AuditAccessFilter,
        queryParams: ManagementAuditEventListQueryParams
    ): AppResult<PagedResult<AuditEventInternal>>
}
