package io.github.mudrichenkoevgeny.backend.core.audit.database.repository

import io.github.mudrichenkoevgeny.backend.core.audit.enums.AuditStatus
import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEvent
import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEventId
import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PagedResponse
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import java.time.Instant
import java.util.UUID

interface AuditEventRepository {
    suspend fun createEvent(event: AuditEvent): AppResult<AuditEvent>

    suspend fun getEventById(auditEventId: AuditEventId): AppResult<AuditEvent?>

    suspend fun getEventsList(
        params: PageParams,
        actorId: UUID? = null,
        action: String? = null,
        resource: String? = null,
        resourceId: String? = null,
        status: AuditStatus? = null,
        fromTimestamp: Instant? = null,
        toTimestamp: Instant? = null
    ): AppResult<PagedResponse<AuditEvent>>

    suspend fun getEventsByActor(
        params: PageParams,
        actorId: UUID
    ): AppResult<PagedResponse<AuditEvent>>
}