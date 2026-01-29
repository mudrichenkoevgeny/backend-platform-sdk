package io.github.mudrichenkoevgeny.backend.core.audit.database.repository

import io.github.mudrichenkoevgeny.backend.core.audit.database.table.AuditEventsTable
import io.github.mudrichenkoevgeny.backend.core.audit.enums.AuditStatus
import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEvent
import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEventId
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PagedResponse
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.extensions.applyPagination
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditEventRepositoryImpl @Inject constructor() : AuditEventRepository {

    override suspend fun createEvent(event: AuditEvent): AppResult<AuditEvent> {
        val inserted = AuditEventsTable.insert { auditEventRow ->
            auditEventRow[id] = event.id.value
            auditEventRow[actorId] = event.actorId
            auditEventRow[action] = event.action
            auditEventRow[resource] = event.resource
            auditEventRow[resourceId] = event.resourceId
            auditEventRow[status] = event.status
            auditEventRow[metadata] = event.metadata
            auditEventRow[message] = event.message
        }

        if (inserted.insertedCount == 0) {
            return AppResult.Error(
                CommonError.Database("Failed to insert audit event: ${event.action}")
            )
        }

        return AppResult.Success(event)
    }

    override suspend fun getEventById(auditEventId: AuditEventId): AppResult<AuditEvent?> {
        val resultRow = AuditEventsTable
            .selectAll()
            .where { AuditEventsTable.id eq auditEventId.value }
            .singleOrNull()

        return AppResult.Success(resultRow?.toAuditEvent())
    }

    override suspend fun getEventsList(
        params: PageParams,
        actorId: UUID?,
        action: String?,
        resource: String?,
        resourceId: String?,
        status: AuditStatus?,
        fromTimestamp: Instant?,
        toTimestamp: Instant?
    ): AppResult<PagedResponse<AuditEvent>> {
        var query = AuditEventsTable.selectAll()

        actorId?.let { id -> query = query.andWhere { AuditEventsTable.actorId eq id } }
        action?.let { act -> query = query.andWhere { AuditEventsTable.action eq act } }
        resource?.let { res -> query = query.andWhere { AuditEventsTable.resource eq res } }
        resourceId?.let { resId -> query = query.andWhere { AuditEventsTable.resourceId eq resId } }
        status?.let { st -> query = query.andWhere { AuditEventsTable.status eq st } }
        fromTimestamp?.let { from -> query = query.andWhere { AuditEventsTable.createdAt greaterEq from } }
        toTimestamp?.let { to -> query = query.andWhere { AuditEventsTable.createdAt lessEq to } }

        val totalCount = query.count()

        val events = query
            .orderBy(AuditEventsTable.createdAt to SortOrder.DESC)
            .applyPagination(params)
            .map { it.toAuditEvent() }

        return AppResult.Success(
            PagedResponse(
                items = events,
                totalCount = totalCount,
                page = params.page,
                size = params.size
            )
        )
    }

    override suspend fun getEventsByActor(params: PageParams, actorId: UUID): AppResult<PagedResponse<AuditEvent>> {
        val query = AuditEventsTable.selectAll().where { AuditEventsTable.actorId eq actorId }

        val totalCount = query.count()

        val events = query
            .orderBy(AuditEventsTable.createdAt to SortOrder.DESC)
            .applyPagination(params)
            .map { it.toAuditEvent() }

        return AppResult.Success(
            PagedResponse(
                items = events,
                totalCount = totalCount,
                page = params.page,
                size = params.size
            )
        )
    }

    private fun ResultRow.toAuditEvent(): AuditEvent = AuditEvent(
        id = AuditEventId(this[AuditEventsTable.id].value),
        actorId = this[AuditEventsTable.actorId],
        action = this[AuditEventsTable.action],
        resource = this[AuditEventsTable.resource],
        resourceId = this[AuditEventsTable.resourceId],
        status = this[AuditEventsTable.status],
        metadata = this[AuditEventsTable.metadata],
        message = this[AuditEventsTable.message]
    )
}