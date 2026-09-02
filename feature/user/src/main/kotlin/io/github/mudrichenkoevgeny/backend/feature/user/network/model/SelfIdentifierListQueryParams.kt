package io.github.mudrichenkoevgeny.backend.feature.user.network.model

import io.github.mudrichenkoevgeny.backend.core.common.pagination.ListingQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues

data class SelfIdentifierListQueryParams(
    val listing: ListingQueryParams<UserSortValues.UserIdentifierSortBy>,
    val userAuthProviders: List<UserAuthProvider>,
    val identifiers: List<String>
)