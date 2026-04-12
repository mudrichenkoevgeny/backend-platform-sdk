package io.github.mudrichenkoevgeny.backend.core.common.pagination

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult

/**
 * Maps each item for API serialization while preserving pagination metadata.
 */
fun <T, R> PagedResult<T>.mapItems(transform: (T) -> R): PagedResult<R> = PagedResult(
    items = items.map(transform),
    totalCount = totalCount,
    pageNumber = pageNumber,
    pageSize = pageSize,
    totalPages = totalPages,
)
