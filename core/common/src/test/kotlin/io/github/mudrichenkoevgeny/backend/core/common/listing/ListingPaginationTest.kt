package io.github.mudrichenkoevgeny.backend.core.common.listing

import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PagedResponse
import io.github.mudrichenkoevgeny.backend.core.common.listing.pagination.model.PaginationConfig
import io.github.mudrichenkoevgeny.backend.core.common.listing.sorting.SortDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ListingPaginationTest {

    @Test
    fun `PageParams offset and limit are derived from page and size`() {
        val params = PageParams(page = 3, size = 10)

        assertEquals(10, params.limit)
        assertEquals(20L, params.offset)
    }

    @Test
    fun `PageParams uses PaginationConfig defaults`() {
        val params = PageParams()

        assertEquals(PaginationConfig.PAGINATION_FIRST_PAGE, params.page)
        assertEquals(PaginationConfig.PAGINATION_PAGE_SIZE, params.size)
    }

    @Test
    fun `PagedResponse totalPages rounds up for remaining items`() {
        val response = PagedResponse(
            items = listOf(1, 2, 3),
            totalCount = 25,
            page = 1,
            size = 10,
        )

        assertEquals(3L, response.totalPages)
    }

    @Test
    fun `PagedResponse totalPages is zero when size is not positive`() {
        val response = PagedResponse(
            items = emptyList<Int>(),
            totalCount = 25,
            page = 1,
            size = 0,
        )

        assertEquals(0L, response.totalPages)
    }

    @Test
    fun `SortDirection has ascending and descending entries`() {
        assertEquals(SortDirection.ASC, SortDirection.valueOf("ASC"))
        assertEquals(SortDirection.DESC, SortDirection.valueOf("DESC"))
    }
}

