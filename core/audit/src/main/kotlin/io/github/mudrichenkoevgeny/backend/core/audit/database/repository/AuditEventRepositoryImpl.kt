package io.github.mudrichenkoevgeny.backend.core.audit.database.repository

import io.github.mudrichenkoevgeny.backend.core.audit.database.table.AuditEventsTable
import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditAccessFilter
import io.github.mudrichenkoevgeny.backend.core.audit.network.model.ManagementAuditEventListQueryParams
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.pagination.getNumOfTotalPages
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.core.common.util.toJavaInstant
import io.github.mudrichenkoevgeny.backend.core.common.util.toKotlinInstant
import io.github.mudrichenkoevgeny.backend.core.database.dsl.applyPagination
import io.github.mudrichenkoevgeny.backend.core.database.dsl.applyTimestampFilter
import io.github.mudrichenkoevgeny.backend.core.database.dsl.substringSqlLikePattern
import io.github.mudrichenkoevgeny.backend.core.database.mapper.toExposedSortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.CompositeAuditActionTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventId
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventInternal
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.listing.AuditEventSortBy
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.CompositeAuditMetadataKeyParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.CompositeAuditResourceTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditEventMetadataPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
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
public class AuditEventRepositoryImpl @Inject constructor(
    private val compositeAuditActionTypeParser: CompositeAuditActionTypeParser,
    private val compositeAuditResourceTypeParser: CompositeAuditResourceTypeParser,
    private val compositeAuditMetadataKeyParser: CompositeAuditMetadataKeyParser
) : AuditEventRepository {
    override suspend fun createEvent(event: AuditEventInternal): AppResult<AuditEventInternal> {
        return try {
            val inserted = AuditEventsTable.insert { auditEventRow ->
                auditEventRow[id] = event.id.value
                auditEventRow[actorId] = event.actorId
                auditEventRow[actorType] = event.actorType
                auditEventRow[actorUserRole] = event.actorUserRole
                auditEventRow[action] = event.action.value
                auditEventRow[resource] = event.resource.value
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
                    CommonError.Database("Failed to insert audit event: ${event.action.value}")
                )
            }

            AppResult.Success(event)
        } catch (e: ExposedSQLException) {
            AppResult.Error(CommonError.Database("Error creating audit event: ${e.message}"))
        }
    }

    override suspend fun getEventById(auditEventId: AuditEventId): AppResult<AuditEventInternal?> = try {
        val resultRow = AuditEventsTable.selectAll().where { AuditEventsTable.id eq auditEventId.value }.singleOrNull()

        AppResult.Success(resultRow?.toAuditEventInternal())
    } catch (e: ExposedSQLException) {
        AppResult.Error(CommonError.Database("Error retrieving audit event by id: ${e.message}"))
    }

    override suspend fun getEventsPageWithAccessFilter(
        accessFilter: AuditAccessFilter,
        queryParams: ManagementAuditEventListQueryParams
    ): AppResult<PagedResult<AuditEventInternal>> {
        return try {
            val simpleTypes = accessFilter.allowedActorTypes.filter { it != AuditActorType.USER }
            val allowsUser = accessFilter.allowedActorTypes.contains(AuditActorType.USER)

            if (
                accessFilter.allowedActorTypes.isEmpty() ||
                (allowsUser && simpleTypes.isEmpty() && accessFilter.allowedUserRoles.isEmpty())
            ) {
                return AppResult.Success(
                    PagedResult(
                        items = emptyList(),
                        totalCount = 0L,
                        pageNumber = queryParams.listing.pageParams.page,
                        pageSize = queryParams.listing.pageParams.size,
                        totalPages = 0L
                    )
                )
            }

            val query = AuditEventsTable.selectAll()

            query.andWhere {
                val conditions = mutableListOf<Op<Boolean>>()

                if (simpleTypes.isNotEmpty()) {
                    conditions.add(AuditEventsTable.actorType inList simpleTypes)
                }

                if (allowsUser && accessFilter.allowedUserRoles.isNotEmpty()) {
                    conditions.add(
                        (AuditEventsTable.actorType eq AuditActorType.USER) and
                            (AuditEventsTable.actorUserRole inList accessFilter.allowedUserRoles)
                    )
                }

                conditions.reduceOrNull { acc, op -> acc or op } ?: Op.FALSE
            }

            if (queryParams.eventIds.isNotEmpty()) {
                query.andWhere { AuditEventsTable.id inList queryParams.eventIds.map { it.value } }
            }
            if (queryParams.actorIds.isNotEmpty()) {
                query.andWhere { AuditEventsTable.actorId inList queryParams.actorIds }
            }
            if (queryParams.actorTypes.isNotEmpty()) {
                query.andWhere { AuditEventsTable.actorType inList queryParams.actorTypes }
            }
            if (queryParams.excludeActorTypes.isNotEmpty()) {
                query.andWhere { AuditEventsTable.actorType notInList queryParams.excludeActorTypes }
            }
            if (queryParams.actorUserRoles.isNotEmpty()) {
                query.andWhere { AuditEventsTable.actorUserRole inList queryParams.actorUserRoles }
            }
            if (queryParams.actions.isNotEmpty()) {
                query.andWhere { AuditEventsTable.action inList queryParams.actions.map { it.value } }
            }
            if (queryParams.excludeActions.isNotEmpty()) {
                query.andWhere { AuditEventsTable.action notInList queryParams.excludeActions.map { it.value } }
            }
            if (queryParams.resources.isNotEmpty()) {
                query.andWhere { AuditEventsTable.resource inList queryParams.resources.map { it.value } }
            }
            if (queryParams.resourceIds.isNotEmpty()) {
                query.andWhere { AuditEventsTable.resourceId inList queryParams.resourceIds }
            }
            if (queryParams.statuses.isNotEmpty()) {
                query.andWhere { AuditEventsTable.status inList queryParams.statuses }
            }
            if (queryParams.excludeStatuses.isNotEmpty()) {
                query.andWhere { AuditEventsTable.status notInList queryParams.excludeStatuses }
            }

            query.applyTimestampFilter(
                column = AuditEventsTable.createdAt,
                fromInstant = queryParams.createdAtFrom,
                toInstant = queryParams.createdAtTo
            )

            val searchQuery = queryParams.searchQuery
            if (!searchQuery.isNullOrBlank()) {
                val pattern = substringSqlLikePattern(searchQuery.lowercase())
                query.andWhere { AuditEventsTable.message.lowerCase() like pattern }
            }

            val totalCount = query.count()

            val sortColumn = when (queryParams.listing.sortBy) {
                AuditEventSortBy.CREATED_AT -> AuditEventsTable.createdAt
            }
            val exposedSortOrder = queryParams.listing.sortOrder.toExposedSortOrder()
            val pageParams = queryParams.listing.pageParams

            val events = query
                .orderBy(sortColumn to exposedSortOrder)
                .applyPagination(pageParams)
                .map { resultRow -> resultRow.toAuditEventInternal() }

            val totalPages = getNumOfTotalPages(totalCount, pageParams.size)

            AppResult.Success(
                PagedResult(
                    items = events,
                    totalCount = totalCount,
                    pageNumber = pageParams.page,
                    pageSize = pageParams.size,
                    totalPages = totalPages
                )
            )
        } catch (e: ExposedSQLException) {
            AppResult.Error(CommonError.Database("Error executing audit events paged query: ${e.message}"))
        }
    }

    private fun ResultRow.toAuditEventInternal(): AuditEventInternal = AuditEventInternal(
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
