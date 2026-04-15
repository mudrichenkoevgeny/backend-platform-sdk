package io.github.mudrichenkoevgeny.backend.feature.user.network.query

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseListingQueryParams
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.getQueryValues
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.IdentifierListQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.toUserIdOrThrow
import io.ktor.server.application.ApplicationCall

/**
 * Parses user-identifier management list query parameters.
 */
fun ApplicationCall.parseIdentifiersListQueryParams(): IdentifierListQueryParams {
    val listing = parseListingQueryParams(
        defaultSortBy = UserSortValues.UserIdentifierSortBy.CREATED_AT,
        parseSortByOrNull = UserSortValues.UserIdentifierSortBy::fromValueOrNull
    )

    val filterNames = UserFilterValues.UserIdentifierFilterValues

    val queryUserIds = getQueryValues(filterNames.USER_ID)
    if (queryUserIds.isEmpty()) {
        throw RequestHandlingException(CommonError.MissingRequiredParameter(filterNames.USER_ID))
    }
    val userIds = queryUserIds.map { userId ->
        runCatching { userId.toUserIdOrThrow() }.getOrElse {
            throw RequestHandlingException(CommonError.InvalidParameterValue(filterNames.USER_ID))
        }
    }

    val userAuthProviders = getQueryValues(filterNames.USER_AUTH_PROVIDER)
        .map { userAuthProvider ->
            UserAuthProvider.fromValueOrNull(userAuthProvider) ?: throw RequestHandlingException(
                CommonError.InvalidParameterValue(filterNames.USER_AUTH_PROVIDER)
            )
        }

    val identifiers = getQueryValues(filterNames.IDENTIFIER)

    return IdentifierListQueryParams(
        listing = listing,
        userIds = userIds,
        userAuthProviders = userAuthProviders,
        identifiers = identifiers
    )
}
