package io.github.mudrichenkoevgeny.backend.feature.user.network.query

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.firstNonBlankQueryValue
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.getQueryValues
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseListingQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.UserListQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.ktor.server.application.ApplicationCall

/**
 * Parses user management list query parameters.
 */
fun ApplicationCall.parseUsersListQueryParams(): UserListQueryParams {
    val listing = parseListingQueryParams(
        defaultSortBy = UserSortValues.UserSortBy.CREATED_AT,
        parseSortByOrNull = UserSortValues.UserSortBy::fromValueOrNull
    )

    val filterNames = UserFilterValues.UserFilterValues

    val roles = getQueryValues(filterNames.ROLE)
        .map { roleValue ->
            UserRole.fromValueOrNull(roleValue) ?: throw RequestHandlingException(
                CommonError.InvalidParameterValue(filterNames.ROLE)
            )
        }

    val accountStatuses = getQueryValues(filterNames.ACCOUNT_STATUS)
        .map { statusValue ->
            UserAccountStatus.fromValueOrNull(statusValue) ?: throw RequestHandlingException(
                CommonError.InvalidParameterValue(filterNames.ACCOUNT_STATUS)
            )
        }

    val accountStatusesBeforeDeletion = getQueryValues(filterNames.ACCOUNT_STATUS_BEFORE_DELETION)
        .map { statusValue ->
            UserAccountStatus.fromValueOrNull(statusValue) ?: throw RequestHandlingException(
                CommonError.InvalidParameterValue(filterNames.ACCOUNT_STATUS_BEFORE_DELETION)
            )
        }

    val authorityLevelFrom = firstNonBlankQueryValue(filterNames.AUTHORITY_LEVEL_FROM)?.toIntOrNull()
    val authorityLevelTo = firstNonBlankQueryValue(filterNames.AUTHORITY_LEVEL_TO)?.toIntOrNull()

    val requiredPermissionCodes = getQueryValues(filterNames.PERMISSION_CODES)
        .map { PermissionCode(it) }
        .toSet()

    val isTotpEnabled = firstNonBlankQueryValue(filterNames.IS_TOTP_ENABLED)?.toBooleanStrictOrNull()

    return UserListQueryParams(
        listing = listing,
        roles = roles,
        accountStatuses = accountStatuses,
        accountStatusesBeforeDeletion = accountStatusesBeforeDeletion,
        authorityLevelFrom = authorityLevelFrom,
        authorityLevelTo = authorityLevelTo,
        requiredPermissionCodes = requiredPermissionCodes,
        isTotpEnabled = isTotpEnabled
    )
}