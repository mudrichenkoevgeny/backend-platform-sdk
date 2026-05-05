package io.github.mudrichenkoevgeny.backend.core.audit.database.repository

import io.github.mudrichenkoevgeny.backend.core.audit.database.table.AuditEventsTable
import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditAccessFilter
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.pagination.getNumOfTotalPages
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.core.common.util.toJavaInstant
import io.github.mudrichenkoevgeny.backend.core.common.util.toKotlinInstant
import io.github.mudrichenkoevgeny.backend.core.database.extensions.applyPagination
import io.github.mudrichenkoevgeny.backend.core.database.extensions.substringSqlLikePattern
import io.github.mudrichenkoevgeny.backend.core.database.mapper.toExposedSortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.CompositeAuditActionTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventId
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.listing.AuditSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.CompositeAuditMetadataKeyParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.CompositeAuditResourceTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditEventMetadataPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exposed-based implementation of [AuditEventRepository].
 *
 * Uses composite parsers to hydrate persisted action, resource, and metadata keys.
 */
@Singleton
class AuditEventRepositoryImpl @Inject constructor(
    private val compositeAuditActionTypeParser: CompositeAuditActionTypeParser,
    private val compositeAuditResourceTypeParser: CompositeAuditResourceTypeParser,
    private val compositeAuditMetadataKeyParser: CompositeAuditMetadataKeyParser
) : AuditEventRepository {

    override suspend fun createEvent(event: AuditEvent): AppResult<AuditEvent> {
        val inserted = AuditEventsTable.insert { auditEventRow ->
            auditEventRow[id] = event.id.value
            auditEventRow[actorId] = event.actorId
            auditEventRow[actorType] = event.actorType
            auditEventRow[actorUserRole] = event.actorUserRole
            auditEventRow[action] = event.action.serialName
            auditEventRow[resource] = event.resource.serialName
            auditEventRow[resourceId] = event.resourceId
            auditEventRow[resourceValueSensitivity] = event.resourceValueSensitivity
            auditEventRow[status] = event.status
            auditEventRow[metadata] = event.metadata.mapToSet { auditEventMetadata ->
                auditEventMetadata.toAuditEventMetadataPayload()
            }
            auditEventRow[message] = event.message
            auditEventRow[createdAt] = event.createdAt.toJavaInstant()
        }

        if (inserted.insertedCount == 0) {
            return AppResult.Error(
                CommonError.Database("Failed to insert audit event: ${event.action.serialName}")
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

    override suspend fun getEventsPageWithAccessFilter(
        accessFilter: AuditAccessFilter,
        pageParams: PageParams,
        sortBy: AuditSortValues.AuditEventSortBy,
        sortOrder: SortOrder,
        actorIds: List<String>,
        actorTypes: List<AuditActorType>,
        actorUserRoles: List<String>,
        actions: List<AuditActionType>,
        resources: List<AuditResourceType>,
        resourceIds: List<String>,
        statuses: List<AuditStatus>,
        messages: List<String>
    ): AppResult<PagedResult<AuditEvent>> {
        var query = AuditEventsTable.selectAll()

        query = query.andWhere {
            val conditions = mutableListOf<Op<Boolean>>()

            val simpleTypes = accessFilter.allowedActorTypes.filter { it != AuditActorType.USER }
            if (simpleTypes.isNotEmpty()) {
                conditions.add(AuditEventsTable.actorType inList simpleTypes)
            }

            if (accessFilter.allowedActorTypes.contains(AuditActorType.USER)) {
                val roles = accessFilter.allowedUserRoles
                if (roles.isNotEmpty()) {
                    conditions.add(
                        (AuditEventsTable.actorType eq AuditActorType.USER) and
                                (AuditEventsTable.actorUserRole inList roles)
                    )
                }
            }

            conditions.reduceOrNull { acc, op -> acc or op } ?: Op.FALSE
        }

        if (actorIds.isNotEmpty()) {
            query = query.andWhere { AuditEventsTable.actorId inList actorIds }
        }
        if (actorTypes.isNotEmpty()) {
            query = query.andWhere { AuditEventsTable.actorType inList actorTypes }
        }
        if (actorUserRoles.isNotEmpty()) {
            query = query.andWhere { AuditEventsTable.actorUserRole inList actorUserRoles }
        }
        if (actions.isNotEmpty()) {
            query = query.andWhere { AuditEventsTable.action inList actions.map { it.serialName } }
        }
        if (resources.isNotEmpty()) {
            query = query.andWhere { AuditEventsTable.resource inList resources.map { it.serialName } }
        }
        if (resourceIds.isNotEmpty()) {
            query = query.andWhere { AuditEventsTable.resourceId inList resourceIds }
        }
        if (statuses.isNotEmpty()) {
            query = query.andWhere { AuditEventsTable.status inList statuses }
        }

        messages.filter { it.isNotBlank() }.forEach { needle ->
            val pattern = substringSqlLikePattern(needle.lowercase())
            query = query.andWhere { AuditEventsTable.message.lowerCase() like pattern }
        }

        val totalCount = query.count()

        val sortColumn = when (sortBy) {
            AuditSortValues.AuditEventSortBy.CREATED_AT -> AuditEventsTable.createdAt
        }
        val exposedSortOrder = sortOrder.toExposedSortOrder()

        val events = query
            .orderBy(sortColumn to exposedSortOrder)
            .applyPagination(pageParams)
            .map { it.toAuditEvent() }

        val totalPages = getNumOfTotalPages(totalCount, pageParams.size)

        return AppResult.Success(
            PagedResult(
                items = events,
                totalCount = totalCount,
                pageNumber = pageParams.page,
                pageSize = pageParams.size,
                totalPages = totalPages
            )
        )
    }

    private fun ResultRow.toAuditEvent(): AuditEvent = AuditEvent(
        id = AuditEventId(this[AuditEventsTable.id].value),
        actorId = this[AuditEventsTable.actorId],
        actorType = this[AuditEventsTable.actorType],
        actorUserRole = this[AuditEventsTable.actorUserRole],
        action = compositeAuditActionTypeParser.fromValueOrThrow(this[AuditEventsTable.action]),
        resource = compositeAuditResourceTypeParser.fromValueOrThrow(this[AuditEventsTable.resource]),
        resourceId = this[AuditEventsTable.resourceId],
        resourceValueSensitivity = this[AuditEventsTable.resourceValueSensitivity],
        status = this[AuditEventsTable.status],
        metadata = this[AuditEventsTable.metadata].mapToSet { auditEventMetadataPayload ->
            auditEventMetadataPayload.toAuditEventMetadata(compositeAuditMetadataKeyParser)
        },
        message = this[AuditEventsTable.message],
        createdAt = this[AuditEventsTable.createdAt].toKotlinInstant()
    )
}