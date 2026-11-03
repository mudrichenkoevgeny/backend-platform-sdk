package io.github.mudrichenkoevgeny.backend.feature.user.network.model

import io.github.mudrichenkoevgeny.backend.core.common.pagination.ListingQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserIdentifierSortBy
import kotlin.time.Instant

/**
 * Query parameters for self-service user identifiers list endpoint.
 */
data class SelfIdentifierListQueryParams(
    val listing: ListingQueryParams<UserIdentifierSortBy>,
    val identifierIds: List<String> = emptyList(),
    val userAuthProviders: List<UserAuthProvider> = emptyList(),
    val excludeUserAuthProviders: List<UserAuthProvider> = emptyList(),
    val identifier: String? = null,
    val displayName: String? = null,
    val externalProviderEmail: String? = null,
    val isExternalProviderEmailSet: Boolean? = null,
    val createdAtFrom: Instant? = null,
    val createdAtTo: Instant? = null,
    val updatedAtFrom: Instant? = null,
    val updatedAtTo: Instant? = null,
    val isUpdatedAtSet: Boolean? = null,
    val searchQuery: String? = null
)
