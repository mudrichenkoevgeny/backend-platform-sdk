package io.github.mudrichenkoevgeny.backend.feature.audit.api.network.query

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.validation.ValidationException
import io.github.mudrichenkoevgeny.backend.core.common.validation.firstNonBlankQueryValue
import io.github.mudrichenkoevgeny.backend.core.common.validation.parseListingQueryParams
import io.github.mudrichenkoevgeny.backend.feature.audit.api.network.model.AuditEventsListQueryParams
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
    compositeAuditResourceTypeParser: CompositeAuditResourceTypeParser,
): AuditEventsListQueryParams {
    val listing = parseListingQueryParams(
        defaultSortBy = AuditSortValues.AuditEventSortBy.CREATED_AT,
        parseSortByOrNull = AuditSortValues.AuditEventSortBy::fromValueOrNull,
    )

    val auditEventFilterParamNames = AuditFilterValues.AuditEventFilterValues

    val actorId = firstNonBlankQueryValue(auditEventFilterParamNames.ACTOR_ID)
    val actorType = firstNonBlankQueryValue(auditEventFilterParamNames.ACTOR_TYPE)?.let { raw ->
        AuditActorType.fromValueOrNull(raw)
            ?: throw ValidationException(
                CommonError.InvalidParameterValue(auditEventFilterParamNames.ACTOR_TYPE)
            )
    }
    val actorUserRole = firstNonBlankQueryValue(auditEventFilterParamNames.ACTOR_USER_ROLE)
    val action = firstNonBlankQueryValue(auditEventFilterParamNames.ACTION)?.let { raw ->
        try {
            compositeAuditActionTypeParser.fromValueOrThrow(raw)
        } catch (_: Exception) {
            throw ValidationException(
                CommonError.InvalidParameterValue(auditEventFilterParamNames.ACTION)
            )
        }
    }
    val resource = firstNonBlankQueryValue(auditEventFilterParamNames.RESOURCE)?.let { raw ->
        try {
            compositeAuditResourceTypeParser.fromValueOrThrow(raw)
        } catch (_: Exception) {
            throw ValidationException(
                CommonError.InvalidParameterValue(auditEventFilterParamNames.RESOURCE)
            )
        }
    }
    val resourceId = firstNonBlankQueryValue(auditEventFilterParamNames.RESOURCE_ID)
    val status = firstNonBlankQueryValue(auditEventFilterParamNames.STATUS)?.let { raw ->
        AuditStatus.fromValueOrNull(raw)
            ?: throw ValidationException(
                CommonError.InvalidParameterValue(auditEventFilterParamNames.STATUS)
            )
    }
    val message = firstNonBlankQueryValue(auditEventFilterParamNames.MESSAGE)

    return AuditEventsListQueryParams(
        listing = listing,
        actorId = actorId,
        actorType = actorType,
        actorUserRole = actorUserRole,
        action = action,
        resource = resource,
        resourceId = resourceId,
        status = status,
        message = message,
    )
}
