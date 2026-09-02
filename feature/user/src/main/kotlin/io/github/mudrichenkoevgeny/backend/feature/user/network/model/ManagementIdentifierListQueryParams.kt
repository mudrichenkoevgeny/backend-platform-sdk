package io.github.mudrichenkoevgeny.backend.feature.user.network.model

import io.github.mudrichenkoevgeny.backend.core.common.pagination.ListingQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

/**
 * Query parameters for management user identifiers list endpoint.
 */
data class ManagementIdentifierListQueryParams(
    val listing: ListingQueryParams<UserSortValues.UserIdentifierSortBy>,
    val userIds: List<UserId>,
    val userAuthProviders: List<UserAuthProvider>,
    val identifiers: List<String>
)
