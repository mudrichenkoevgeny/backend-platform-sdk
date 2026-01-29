package io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model

data class PagedResponse<T>(
    val items: List<T>,
    val totalCount: Long,
    val page: Int,
    val size: Int
) {
    val totalPages: Long = if (size > 0) {
        (totalCount + size - 1) / size
    } else {
        0
    }
}