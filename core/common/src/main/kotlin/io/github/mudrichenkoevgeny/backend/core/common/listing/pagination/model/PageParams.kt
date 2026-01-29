package io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model

import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.constants.PaginationConstants

data class PageParams(
    val page: Int = PaginationConstants.PAGINATION_FIRST_PAGE,
    val size: Int = PaginationConstants.PAGINATION_PAGE_SIZE
) {
    val limit: Int get() = size
    val offset: Long get() = ((page - 1) * size).toLong()
}