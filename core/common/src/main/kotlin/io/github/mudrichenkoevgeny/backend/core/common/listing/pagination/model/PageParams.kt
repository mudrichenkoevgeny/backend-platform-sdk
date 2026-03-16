package io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model

/**
 * Basic pagination request parameters.
 *
 * These values are typically derived from query parameters and translated
 * into database `LIMIT` / `OFFSET` semantics via [limit] and [offset].
 *
 * @param page 1-based page number.
 * @param size number of items per page.
 */
data class PageParams(
    val page: Int = PaginationConfig.PAGINATION_FIRST_PAGE,
    val size: Int = PaginationConfig.PAGINATION_PAGE_SIZE
) {
    /**
     * Maximum number of items requested for the page.
     */
    val limit: Int get() = size

    /**
     * Zero-based offset from which to start reading items.
     */
    val offset: Long get() = ((page - 1) * size).toLong()
}