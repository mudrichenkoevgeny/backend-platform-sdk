package io.github.mudrichenkoevgeny.backend.core.audit.manager

import io.github.mudrichenkoevgeny.backend.core.audit.database.repository.AuditEventRepository
import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditStatus
import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEvent
import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEventId
import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PagedResponse
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.util.dbQuery
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Singleton
class AuditManagerImpl @Inject constructor(
    private val auditRepository: AuditEventRepository
) : AuditManager {

    override suspend fun createEvent(auditEvent: AuditEvent): AppResult<AuditEvent> = dbQuery {
        auditRepository.createEvent(auditEvent)
    }

    override suspend fun getEventById(eventId: AuditEventId): AppResult<AuditEvent?> = dbQuery {
        auditRepository.getEventById(eventId)
    }

    override suspend fun getEventsList(
        params: PageParams,
        actorId: Uuid?,
        action: String?,
        resource: String?,
        resourceId: String?,
        status: AuditStatus?,
        fromTimestamp: Instant?,
        toTimestamp: Instant?
    ): AppResult<PagedResponse<AuditEvent>> = dbQuery {
        auditRepository.getEventsList(
            params = params,
            actorId = actorId,
            action = action,
            resource = resource,
            resourceId = resourceId,
            status = status,
            fromTimestamp = fromTimestamp,
            toTimestamp = toTimestamp
        )
    }
}