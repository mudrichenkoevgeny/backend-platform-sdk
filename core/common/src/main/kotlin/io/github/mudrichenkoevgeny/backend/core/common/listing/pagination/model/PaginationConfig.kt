package io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model

/**
 * Default pagination settings used across listing endpoints.
 */
object PaginationConfig {

    /**
     * Default 1-based index of the first page.
     */
    const val PAGINATION_FIRST_PAGE = 1

    /**
     * Default number of items per page when the client does not override it.
     */
    const val PAGINATION_PAGE_SIZE = 20
}