package io.github.mudrichenkoevgeny.backend.core.audit.network.query

import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.firstNonBlankQueryValue
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.getQueryValues
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseTimestampQueryOrNull
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseValidatedQueryList
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseListingQueryParams
import io.github.mudrichenkoevgeny.backend.core.audit.network.model.ManagementAuditEventListQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.CompositeAuditActionTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.toAuditEventIdOrNull
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.listing.AuditEventFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.listing.AuditEventSortBy
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.CompositeAuditResourceTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.ktor.server.application.ApplicationCall

/**
 * Parses audit-specific list query parameters with strict validation into [ManagementAuditEventListQueryParams].
 *
 * @param compositeAuditActionTypeParser parser used to validate and convert action wire strings into [AuditActionType] instances
 * @param compositeAuditResourceTypeParser parser used to validate and convert resource wire strings into [AuditResourceType] instances
 * @return [ManagementAuditEventListQueryParams] containing strictly validated query parameters and listing settings
 * @throws io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
 * when any query parameter fails validation or parsing (e.g. invalid UUID, enum, or timestamp format)
 */
fun ApplicationCall.parseAuditEventsListQueryParams(
    compositeAuditActionTypeParser: CompositeAuditActionTypeParser,
    compositeAuditResourceTypeParser: CompositeAuditResourceTypeParser
): ManagementAuditEventListQueryParams {
    val listing = parseListingQueryParams(
        defaultSortBy = AuditEventSortBy.DEFAULT,
        parseSortByOrNull = AuditEventSortBy::fromValueOrNull
    )

    val filterNames = AuditEventFilterValues

    val eventIds = parseValidatedQueryList(filterNames.EVENT_ID, String::toAuditEventIdOrNull)
    val actorTypes = parseValidatedQueryList(filterNames.ACTOR_TYPE, AuditActorType::fromValueOrNull)
    val excludeActorTypes = parseValidatedQueryList(filterNames.EXCLUDE_ACTOR_TYPE, AuditActorType::fromValueOrNull)
    val actions = parseValidatedQueryList(filterNames.ACTION) { raw ->
        runCatching { compositeAuditActionTypeParser.fromValueOrThrow(raw) }.getOrNull()
    }
    val excludeActions = parseValidatedQueryList(filterNames.EXCLUDE_ACTION) { raw ->
        runCatching { compositeAuditActionTypeParser.fromValueOrThrow(raw) }.getOrNull()
    }
    val resources = parseValidatedQueryList(filterNames.RESOURCE) { raw ->
        runCatching { compositeAuditResourceTypeParser.fromValueOrThrow(raw) }.getOrNull()
    }
    val statuses = parseValidatedQueryList(filterNames.STATUS, AuditStatus::fromValueOrNull)
    val excludeStatuses = parseValidatedQueryList(filterNames.EXCLUDE_STATUS, AuditStatus::fromValueOrNull)

    return ManagementAuditEventListQueryParams(
        listing = listing,
        eventIds = eventIds,
        actorIds = getQueryValues(filterNames.ACTOR_ID),
        actorTypes = actorTypes,
        excludeActorTypes = excludeActorTypes,
        actorUserRoles = getQueryValues(filterNames.ACTOR_USER_ROLE),
        actions = actions,
        excludeActions = excludeActions,
        resources = resources,
        resourceIds = getQueryValues(filterNames.RESOURCE_ID),
        statuses = statuses,
        excludeStatuses = excludeStatuses,
        createdAtFrom = parseTimestampQueryOrNull(filterNames.CREATED_AT_FROM),
        createdAtTo = parseTimestampQueryOrNull(filterNames.CREATED_AT_TO),
        searchQuery = firstNonBlankQueryValue(filterNames.SEARCH_QUERY)
    )
}
