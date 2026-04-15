package io.github.mudrichenkoevgeny.backend.core.common.pagination

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder

/**
 * Common query slice for list endpoints: pagination plus sort field and direction.
 *
 * Feature routers compose this (e.g. `val listing: ListingQueryParams<MySortBy>`) and add
 * resource-specific filters in their own data classes.
 */
data class ListingQueryParams<SortBy>(
    val pageParams: PageParams,
    val sortBy: SortBy,
    val sortOrder: SortOrder
)
