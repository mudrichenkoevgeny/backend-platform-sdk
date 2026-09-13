package io.github.mudrichenkoevgeny.backend.core.common.network.request.handler

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.ListingParamNames
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.ktor.http.Parameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NetworkCallQueryListingParserTest {

    private val defaultSort = TestSort.DEFAULT

    @Test
    fun `parseListingQueryParams uses defaults when query empty`() {
        val call = createTestApplicationCallWithQuery(Parameters.build { })

        val listing = call.parseListingQueryParams(
            defaultSortBy = defaultSort,
            parseSortByOrNull = TestSort::fromWireOrNull
        )

        assertEquals(PageParams(page = 1, size = PageParams.DEFAULT_PAGE_SIZE), listing.pageParams)
        assertEquals(defaultSort, listing.sortBy)
        assertEquals(SortOrder.DESC, listing.sortOrder)
    }

    @Test
    fun `parseListingQueryParams reads page size sort_by and sort_order`() {
        val call = createTestApplicationCallWithQuery(
            Parameters.build {
                append(ListingParamNames.Pagination.PAGE_NUMBER, "2")
                append(ListingParamNames.Pagination.PAGE_SIZE, "15")
                append(ListingParamNames.Sort.SORT_BY, "custom")
                append(ListingParamNames.Sort.SORT_ORDER, wireFor(SortOrder.ASC))
            }
        )

        val listing = call.parseListingQueryParams(
            defaultSortBy = defaultSort,
            parseSortByOrNull = TestSort::fromWireOrNull
        )

        assertEquals(PageParams(page = 2, size = 15), listing.pageParams)
        assertEquals(TestSort.CUSTOM, listing.sortBy)
        assertEquals(SortOrder.ASC, listing.sortOrder)
    }

    @Test
    fun `parseListingQueryParams throws when page not positive`() {
        val call = createTestApplicationCallWithQuery(
            Parameters.build { append(ListingParamNames.Pagination.PAGE_NUMBER, "0") }
        )

        assertThrows(RequestHandlingException::class.java) {
            call.parseListingQueryParams(
                defaultSortBy = defaultSort,
                parseSortByOrNull = TestSort::fromWireOrNull
            )
        }
    }

    @Test
    fun `parseListingQueryParams throws when page size not positive`() {
        val call = createTestApplicationCallWithQuery(
            Parameters.build { append(ListingParamNames.Pagination.PAGE_SIZE, "0") }
        )

        assertThrows(RequestHandlingException::class.java) {
            call.parseListingQueryParams(
                defaultSortBy = defaultSort,
                parseSortByOrNull = TestSort::fromWireOrNull
            )
        }
    }

    @Test
    fun `parseListingQueryParams throws when sort_by unknown`() {
        val call = createTestApplicationCallWithQuery(
            Parameters.build { append(ListingParamNames.Sort.SORT_BY, "unknown") }
        )

        val ex = assertThrows(RequestHandlingException::class.java) {
            call.parseListingQueryParams(
                defaultSortBy = defaultSort,
                parseSortByOrNull = TestSort::fromWireOrNull
            )
        }
        assertEquals(CommonError.InvalidParameterValue::class, ex.error::class)
    }

    @Test
    fun `parseListingQueryParams throws when sort_order unknown`() {
        val call = createTestApplicationCallWithQuery(
            Parameters.build { append(ListingParamNames.Sort.SORT_ORDER, "sideways") }
        )

        val ex = assertThrows(RequestHandlingException::class.java) {
            call.parseListingQueryParams(
                defaultSortBy = defaultSort,
                parseSortByOrNull = TestSort::fromWireOrNull
            )
        }
        assertEquals(CommonError.InvalidParameterValue::class, ex.error::class)
    }

    private fun wireFor(sortOrder: SortOrder): String {
        val candidates = listOf(
            sortOrder.name,
            sortOrder.name.lowercase(),
            sortOrder.name.uppercase()
        )
        return candidates.firstOrNull { SortOrder.fromValueOrNull(it) == sortOrder }
            ?: error("No wire string maps to $sortOrder via SortOrder.fromValueOrNull")
    }
}
