package com.mudrichenkoevgeny.backend.core.audit.manager

import com.mudrichenkoevgeny.backend.core.audit.enums.AuditStatus
import com.mudrichenkoevgeny.backend.core.audit.model.AuditEvent
import com.mudrichenkoevgeny.backend.core.audit.model.AuditEventId
import com.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PageParams
import com.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PagedResponse
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
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