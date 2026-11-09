package io.github.mudrichenkoevgeny.backend.feature.user.network.query

import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.firstNonBlankQueryValue
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseBooleanQueryOrNull
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseListingQueryParams
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseTimestampQueryOrNull
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseValidatedQueryList
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.SelfSessionListQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSessionFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSessionSortBy
import io.ktor.server.application.ApplicationCall
import kotlin.uuid.Uuid

/**
 * Parses self-service user session list query parameters with strict validation.
 */
public fun ApplicationCall.parseSelfSessionsListQueryParams(): SelfSessionListQueryParams {
    val filterNames = UserSessionFilterValues

    return SelfSessionListQueryParams(
        listing = parseListingQueryParams(
            defaultSortBy = UserSessionSortBy.DEFAULT,
            parseSortByOrNull = UserSessionSortBy::fromValueOrNull
        ),
        sessionIds = parseValidatedQueryList(filterNames.SESSION_ID) { Uuid.parseOrNull(it)?.toString() },
        identifier = firstNonBlankQueryValue(filterNames.IDENTIFIER),
        identifierIds = parseValidatedQueryList(filterNames.IDENTIFIER_ID) { Uuid.parseOrNull(it)?.toString() },
        identifierDisplayName = firstNonBlankQueryValue(filterNames.IDENTIFIER_DISPLAY_NAME),
        identifierAuthProviders = parseValidatedQueryList(
            filterNames.IDENTIFIER_AUTH_PROVIDER,
            UserAuthProvider::fromValueOrNull
        ),
        excludeIdentifierAuthProviders = parseValidatedQueryList(
            filterNames.EXCLUDE_IDENTIFIER_AUTH_PROVIDER,
            UserAuthProvider::fromValueOrNull
        ),
        clientTypes = parseValidatedQueryList(
            filterNames.CLIENT_TYPE,
            ClientType::fromValueOrNull
        ),
        excludeClientTypes = parseValidatedQueryList(
            filterNames.EXCLUDE_CLIENT_TYPE,
            ClientType::fromValueOrNull
        ),
        isClientTypeSet = parseBooleanQueryOrNull(filterNames.IS_CLIENT_TYPE_SET),
        deviceId = firstNonBlankQueryValue(filterNames.DEVICE_ID),
        isDeviceIdSet = parseBooleanQueryOrNull(filterNames.IS_DEVICE_ID_SET),
        deviceName = firstNonBlankQueryValue(filterNames.DEVICE_NAME),
        isDeviceNameSet = parseBooleanQueryOrNull(filterNames.IS_DEVICE_NAME_SET),
        language = firstNonBlankQueryValue(filterNames.LANGUAGE),
        isLanguageSet = parseBooleanQueryOrNull(filterNames.IS_LANGUAGE_SET),
        appVersion = firstNonBlankQueryValue(filterNames.APP_VERSION),
        isAppVersionSet = parseBooleanQueryOrNull(filterNames.IS_APP_VERSION_SET),
        osVersion = firstNonBlankQueryValue(filterNames.OPERATION_SYSTEM_VERSION),
        isOperationSystemVersionSet = parseBooleanQueryOrNull(filterNames.IS_OPERATION_SYSTEM_VERSION_SET),
        userAgent = firstNonBlankQueryValue(filterNames.USER_AGENT),
        isUserAgentSet = parseBooleanQueryOrNull(filterNames.IS_USER_AGENT_SET),
        ipAddress = firstNonBlankQueryValue(filterNames.IP_ADDRESS),
        isIpAddressSet = parseBooleanQueryOrNull(filterNames.IS_IP_ADDRESS_SET),
        expiresAtFrom = parseTimestampQueryOrNull(filterNames.EXPIRES_AT_FROM),
        expiresAtTo = parseTimestampQueryOrNull(filterNames.EXPIRES_AT_TO),
        lastAccessedAtFrom = parseTimestampQueryOrNull(filterNames.LAST_ACCESSED_AT_FROM),
        lastAccessedAtTo = parseTimestampQueryOrNull(filterNames.LAST_ACCESSED_AT_TO),
        lastReauthenticatedAtFrom = parseTimestampQueryOrNull(filterNames.LAST_REAUTHENTICATED_AT_FROM),
        lastReauthenticatedAtTo = parseTimestampQueryOrNull(filterNames.LAST_REAUTHENTICATED_AT_TO),
        createdAtFrom = parseTimestampQueryOrNull(filterNames.CREATED_AT_FROM),
        createdAtTo = parseTimestampQueryOrNull(filterNames.CREATED_AT_TO),
        updatedAtFrom = parseTimestampQueryOrNull(filterNames.UPDATED_AT_FROM),
        updatedAtTo = parseTimestampQueryOrNull(filterNames.UPDATED_AT_TO),
        isUpdatedAtSet = parseBooleanQueryOrNull(filterNames.IS_UPDATED_AT_SET),
        searchQuery = firstNonBlankQueryValue(filterNames.SEARCH_QUERY)
    )
}
