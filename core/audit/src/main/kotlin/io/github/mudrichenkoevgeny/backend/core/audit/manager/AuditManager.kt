package io.github.mudrichenkoevgeny.backend.core.audit.manager

import io.github.mudrichenkoevgeny.backend.core.audit.enums.AuditStatus
import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEvent
import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEventId
import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PagedResponse
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import java.time.Instant
import java.util.UUID

interface AuditManager {
    suspend fun createEvent(auditEvent: AuditEvent): AppResult<AuditEvent>

    suspend fun getEventById(eventId: AuditEventId): AppResult<AuditEvent?>

    suspend fun getEventsList(
        params: PageParams,
        actorId: UUID?,
        action: String?,
        resource: String?,
        resourceId: String?,
        status: AuditStatus?,
        fromTimestamp: Instant?,
        toTimestamp: Instant?
    ): AppResult<PagedResponse<AuditEvent>>
}