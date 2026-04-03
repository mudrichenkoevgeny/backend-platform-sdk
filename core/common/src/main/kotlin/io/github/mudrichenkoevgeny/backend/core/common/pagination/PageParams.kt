package io.github.mudrichenkoevgeny.backend.core.common.pagination

/**
 * SQL-oriented pagination: 1-based [page] and non-negative [size], exposed as [limit]/[offset] for `LIMIT`/`OFFSET`.
 */
data class PageParams(
    val page: Int,
    val size: Int = DEFAULT_PAGE_SIZE
) {
    val limit: Int
        get() = size.coerceAtLeast(0)

    val offset: Long
        get() {
            val p = page.coerceAtLeast(1)
            return (p - 1L) * limit
        }

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}

/**
 * Number of pages for [totalCount] rows and [pageSize] per page; `0` when [pageSize] is not positive.
 * Matches foundation `PagedResult.totalPages` semantics.
 */
fun getNumOfTotalPages(totalCount: Long, pageSize: Int): Long = if (pageSize <= 0) {
    0L
} else {
    (totalCount + pageSize - 1L) / pageSize
}