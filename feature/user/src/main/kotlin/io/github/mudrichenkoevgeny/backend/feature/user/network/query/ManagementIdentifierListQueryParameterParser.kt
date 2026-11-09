package io.github.mudrichenkoevgeny.backend.feature.user.network.query

import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.firstNonBlankQueryValue
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseBooleanQueryOrNull
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseListingQueryParams
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseTimestampQueryOrNull
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseValidatedQueryList
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.ManagementIdentifierListQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserIdentifierFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserIdentifierSortBy
import io.ktor.server.application.ApplicationCall
import kotlin.uuid.Uuid

/**
 * Parses user-identifier management list query parameters with strict validation.
 */
public fun ApplicationCall.parseManagementIdentifiersListQueryParams(): ManagementIdentifierListQueryParams {
    val filterNames = UserIdentifierFilterValues

    return ManagementIdentifierListQueryParams(
        listing = parseListingQueryParams(
            defaultSortBy = UserIdentifierSortBy.DEFAULT,
            parseSortByOrNull = UserIdentifierSortBy::fromValueOrNull
        ),
        identifierIds = parseValidatedQueryList(filterNames.IDENTIFIER_ID) { Uuid.parseOrNull(it)?.toString() },
        userIds = parseValidatedQueryList(filterNames.USER_ID) { Uuid.parseOrNull(it)?.toString() },
        userAuthProviders = parseValidatedQueryList(
            filterNames.USER_AUTH_PROVIDER,
            UserAuthProvider::fromValueOrNull
        ),
        excludeUserAuthProviders = parseValidatedQueryList(
            filterNames.EXCLUDE_USER_AUTH_PROVIDER,
            UserAuthProvider::fromValueOrNull
        ),
        identifier = firstNonBlankQueryValue(filterNames.IDENTIFIER),
        displayName = firstNonBlankQueryValue(filterNames.DISPLAY_NAME),
        externalProviderEmail = firstNonBlankQueryValue(filterNames.EXTERNAL_PROVIDER_EMAIL),
        isExternalProviderEmailSet = parseBooleanQueryOrNull(filterNames.IS_EXTERNAL_PROVIDER_EMAIL_SET),
        createdAtFrom = parseTimestampQueryOrNull(filterNames.CREATED_AT_FROM),
        createdAtTo = parseTimestampQueryOrNull(filterNames.CREATED_AT_TO),
        updatedAtFrom = parseTimestampQueryOrNull(filterNames.UPDATED_AT_FROM),
        updatedAtTo = parseTimestampQueryOrNull(filterNames.UPDATED_AT_TO),
        isUpdatedAtSet = parseBooleanQueryOrNull(filterNames.IS_UPDATED_AT_SET),
        searchQuery = firstNonBlankQueryValue(filterNames.SEARCH_QUERY)
    )
}
