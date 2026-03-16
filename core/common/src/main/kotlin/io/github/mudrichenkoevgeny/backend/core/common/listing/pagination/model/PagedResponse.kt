package io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model

/**
 * Generic response wrapper for paginated listings.
 *
 * Exposes the current [page], page [size], [items] on that page, and the
 * [totalCount] across all pages. The [totalPages] value is derived from
 * [totalCount] and [size].
 *
 * @param T type of items in the page.
 */
data class PagedResponse<T>(
    val items: List<T>,
    val totalCount: Long,
    val page: Int,
    val size: Int
) {
    /**
     * Total number of pages for the given [totalCount] and [size].
     *
     * Returns `0` when [size] is not positive to avoid division by zero.
     */
    val totalPages: Long = if (size > 0) {
        (totalCount + size - 1) / size
    } else {
        0
    }
}