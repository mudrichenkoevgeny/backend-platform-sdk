package io.github.mudrichenkoevgeny.backend.core.audit.network.model

import io.github.mudrichenkoevgeny.backend.core.common.pagination.ListingQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventId
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.listing.AuditEventSortBy
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import kotlin.time.Instant

/**
 * Query parameters for management audit events list endpoint.
 *
 * Provides strict type safety (e.g. [Instant], [AuditEventId], [AuditActionType], [AuditResourceType])
 * for fields that are represented as strings in the shared network/client library layer.
 */
data class ManagementAuditEventListQueryParams(
    val listing: ListingQueryParams<AuditEventSortBy>,
    val eventIds: List<AuditEventId> = emptyList(),
    val actorIds: List<String> = emptyList(),
    val actorTypes: List<AuditActorType> = emptyList(),
    val excludeActorTypes: List<AuditActorType> = emptyList(),
    val actorUserRoles: List<String> = emptyList(),
    val actions: List<AuditActionType> = emptyList(),
    val excludeActions: List<AuditActionType> = emptyList(),
    val resources: List<AuditResourceType> = emptyList(),
    val resourceIds: List<String> = emptyList(),
    val statuses: List<AuditStatus> = emptyList(),
    val excludeStatuses: List<AuditStatus> = emptyList(),
    val createdAtFrom: Instant? = null,
    val createdAtTo: Instant? = null,
    val searchQuery: String? = null
)
