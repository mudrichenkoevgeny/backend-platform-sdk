package io.github.mudrichenkoevgeny.backend.feature.user.network.query

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseListingQueryParams
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.getQueryValues
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.SessionListQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.toUserIdentifierIdOrThrow
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.toUserIdOrThrow
import io.ktor.server.application.ApplicationCall

/**
 * Parses user-session management list query parameters.
 */
fun ApplicationCall.parseSessionsListQueryParams(): SessionListQueryParams {
    val listing = parseListingQueryParams(
        defaultSortBy = UserSortValues.UserSessionSortBy.CREATED_AT,
        parseSortByOrNull = UserSortValues.UserSessionSortBy::fromValueOrNull
    )
    val filterNames = UserFilterValues.UserSessionFilterValues

    val queryUserIds = getQueryValues(filterNames.USER_ID)
    if (queryUserIds.isEmpty()) {
        throw RequestHandlingException(CommonError.MissingRequiredParameter(filterNames.USER_ID))
    }
    val userIds = queryUserIds.map { userId ->
        runCatching { userId.toUserIdOrThrow() }.getOrElse {
            throw RequestHandlingException(CommonError.InvalidParameterValue(filterNames.USER_ID))
        }
    }

    val identifierIds = getQueryValues(filterNames.IDENTIFIER_ID).map { identifierId ->
        runCatching { identifierId.toUserIdentifierIdOrThrow() }.getOrElse {
            throw RequestHandlingException(CommonError.InvalidParameterValue(filterNames.IDENTIFIER_ID))
        }
    }

    val identifierAuthProviders = getQueryValues(filterNames.USER_AUTH_PROVIDER).map { identifierAuthProvider ->
        UserAuthProvider.fromValueOrNull(identifierAuthProvider)
            ?: throw RequestHandlingException(CommonError.InvalidParameterValue(filterNames.USER_AUTH_PROVIDER))
    }

    val revokedValues = getQueryValues(filterNames.REVOKED).map { revoked ->
        revoked.toBooleanStrictOrNull()
            ?: throw RequestHandlingException(CommonError.InvalidParameterValue(filterNames.REVOKED))
    }

    val clientTypes = getQueryValues(filterNames.CLIENT_TYPE).map { clientType ->
        ClientType.fromValueOrNull(clientType)
            ?: throw RequestHandlingException(CommonError.InvalidParameterValue(filterNames.CLIENT_TYPE))
    }

    return SessionListQueryParams(
        listing = listing,
        userIds = userIds,
        identifiers = getQueryValues(filterNames.IDENTIFIER),
        identifierIds = identifierIds,
        identifierAuthProviders = identifierAuthProviders,
        revokedValues = revokedValues,
        clientTypes = clientTypes,
        userAgents = getQueryValues(filterNames.USER_AGENT),
        ipAddresses = getQueryValues(filterNames.IP_ADDRESS),
        languages = getQueryValues(filterNames.LANGUAGE),
        deviceIds = getQueryValues(filterNames.DEVICE_ID),
        deviceNames = getQueryValues(filterNames.DEVICE_NAME),
        appVersions = getQueryValues(filterNames.APP_VERSION),
        operationSystemVersions = getQueryValues(filterNames.OPERATION_SYSTEM_VERSION)
    )
}
