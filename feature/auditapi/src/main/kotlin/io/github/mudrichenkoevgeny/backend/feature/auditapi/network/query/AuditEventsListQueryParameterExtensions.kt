package io.github.mudrichenkoevgeny.backend.feature.auditapi.network.query

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseListingQueryParams
import io.github.mudrichenkoevgeny.backend.feature.auditapi.api.network.model.AuditEventsListQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.CompositeAuditActionTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.listing.AuditFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.listing.AuditSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.CompositeAuditResourceTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.ktor.server.application.ApplicationCall

/**
 * Parses audit-specific list query parameters (filters wire names from [AuditFilterValues.AuditEventFilterValues]).
 */
fun ApplicationCall.parseAuditEventsListQueryParams(
    compositeAuditActionTypeParser: CompositeAuditActionTypeParser,
    compositeAuditResourceTypeParser: CompositeAuditResourceTypeParser
): AuditEventsListQueryParams {
    val listing = parseListingQueryParams(
        defaultSortBy = AuditSortValues.AuditEventSortBy.CREATED_AT,
        parseSortByOrNull = AuditSortValues.AuditEventSortBy::fromValueOrNull
    )

    val auditEventFilterParamNames = AuditFilterValues.AuditEventFilterValues

    val actorIds = parameters.getAll(auditEventFilterParamNames.ACTOR_ID).orEmpty()
        .filter { it.isNotBlank() }

    val actorTypes = parameters.getAll(auditEventFilterParamNames.ACTOR_TYPE).orEmpty()
        .filter { it.isNotBlank() }
        .map { value ->
            AuditActorType.fromValueOrNull(value)
                ?: throw RequestHandlingException(
                    CommonError.InvalidParameterValue(auditEventFilterParamNames.ACTOR_TYPE)
                )
        }

    val actorUserRoles = parameters.getAll(auditEventFilterParamNames.ACTOR_USER_ROLE).orEmpty()
        .filter { it.isNotBlank() }

    val actions = parameters.getAll(auditEventFilterParamNames.ACTION).orEmpty()
        .filter { it.isNotBlank() }
        .map { value ->
            try {
                compositeAuditActionTypeParser.fromValueOrThrow(value)
            } catch (_: Exception) {
                throw RequestHandlingException(
                    CommonError.InvalidParameterValue(auditEventFilterParamNames.ACTION)
                )
            }
        }

    val resources = parameters.getAll(auditEventFilterParamNames.RESOURCE).orEmpty()
        .filter { it.isNotBlank() }
        .map { value ->
            try {
                compositeAuditResourceTypeParser.fromValueOrThrow(value)
            } catch (_: Exception) {
                throw RequestHandlingException(
                    CommonError.InvalidParameterValue(auditEventFilterParamNames.RESOURCE)
                )
            }
        }

    val resourceIds = parameters.getAll(auditEventFilterParamNames.RESOURCE_ID).orEmpty()
        .filter { it.isNotBlank() }

    val statuses = parameters.getAll(auditEventFilterParamNames.STATUS).orEmpty()
        .filter { it.isNotBlank() }
        .map { value ->
            AuditStatus.fromValueOrNull(value)
                ?: throw RequestHandlingException(
                    CommonError.InvalidParameterValue(auditEventFilterParamNames.STATUS)
                )
        }

    val messages = parameters.getAll(auditEventFilterParamNames.MESSAGE).orEmpty()
        .filter { it.isNotBlank() }

    return AuditEventsListQueryParams(
        listing = listing,
        actorIds = actorIds,
        actorTypes = actorTypes,
        actorUserRoles = actorUserRoles,
        actions = actions,
        resources = resources,
        resourceIds = resourceIds,
        statuses = statuses,
        messages = messages
    )
}