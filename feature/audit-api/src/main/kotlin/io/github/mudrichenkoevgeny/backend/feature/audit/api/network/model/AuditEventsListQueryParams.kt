package io.github.mudrichenkoevgeny.backend.feature.audit.api.network.model

import io.github.mudrichenkoevgeny.backend.core.common.pagination.ListingQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.listing.AuditSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus

/**
 * Query parameters for the management audit events list endpoint: shared listing slice plus audit filters.
 */
data class AuditEventsListQueryParams(
    val listing: ListingQueryParams<AuditSortValues.AuditEventSortBy>,
    val actorId: String?,
    val actorType: AuditActorType?,
    val actorUserRole: String?,
    val action: AuditActionType?,
    val resource: AuditResourceType?,
    val resourceId: String?,
    val status: AuditStatus?,
    val message: String?,
)