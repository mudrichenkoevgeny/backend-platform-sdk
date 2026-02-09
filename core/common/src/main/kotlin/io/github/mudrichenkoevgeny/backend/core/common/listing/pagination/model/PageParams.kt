package io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model

data class PageParams(
    val page: Int = PaginationConfig.PAGINATION_FIRST_PAGE,
    val size: Int = PaginationConfig.PAGINATION_PAGE_SIZE
) {
    val limit: Int get() = size
    val offset: Long get() = ((page - 1) * size).toLong()
}