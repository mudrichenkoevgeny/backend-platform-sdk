package io.github.mudrichenkoevgeny.backend.core.audit.manager

import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditStatus
import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEvent
import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEventId
import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PagedResponse
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import java.time.Instant
import kotlin.uuid.Uuid

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
     */
    suspend fun getEventById(eventId: AuditEventId): AppResult<AuditEvent?>

    /**
     * Returns a paginated list of audit events matching the given filters.
     * All filter parameters are optional; null means "no filter".
     *
     * @param params Pagination (page, size).
     * @param actorId Filter by actor UUID.
     * @param action Filter by action name.
     * @param resource Filter by resource type.
     * @param resourceId Filter by resource id.
     * @param status Filter by [AuditStatus].
     * @param fromTimestamp Events from this time (inclusive).
     * @param toTimestamp Events up to this time (inclusive).
     */
    suspend fun getEventsList(
        params: PageParams,
        actorId: Uuid?,
        action: String?,
        resource: String?,
        resourceId: String?,
        status: AuditStatus?,
        fromTimestamp: Instant?,
        toTimestamp: Instant?
    ): AppResult<PagedResponse<AuditEvent>>
}