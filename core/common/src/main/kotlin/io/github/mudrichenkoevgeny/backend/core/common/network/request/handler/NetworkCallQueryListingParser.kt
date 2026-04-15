package io.github.mudrichenkoevgeny.backend.core.common.network.request.handler

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.pagination.ListingQueryParams
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.ListingParamNames
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.ktor.server.application.ApplicationCall

/**
 * Reads standard listing query parameters ([ListingParamNames]): page, page size, sort field, sort order.
 *
 * @param defaultSortBy used when `sort_by` is omitted
 * @param parseSortByOrNull returns null when the raw `sort_by` value is not recognized
 * @throws RequestHandlingException when a present parameter has an invalid value
 */
fun <SortBy> ApplicationCall.parseListingQueryParams(
    defaultSortBy: SortBy,
    parseSortByOrNull: (String) -> SortBy?
): ListingQueryParams<SortBy> {
    val pageNumber = parsePositiveIntQuery(
        paramName = ListingParamNames.Pagination.PAGE_NUMBER,
        default = 1
    )
    val pageSize = parsePositiveIntQuery(
        paramName = ListingParamNames.Pagination.PAGE_SIZE,
        default = PageParams.DEFAULT_PAGE_SIZE
    )

    val sortByRaw = firstNonBlankQueryValue(ListingParamNames.Sort.SORT_BY)
    val sortBy = if (sortByRaw == null) {
        defaultSortBy
    } else {
        parseSortByOrNull(sortByRaw)
            ?: throw RequestHandlingException(CommonError.InvalidParameterValue(ListingParamNames.Sort.SORT_BY))
    }

    val sortOrderRaw = firstNonBlankQueryValue(ListingParamNames.Sort.SORT_ORDER)
    val sortOrder = if (sortOrderRaw == null) {
        SortOrder.DESC
    } else {
        SortOrder.fromValueOrNull(sortOrderRaw)
            ?: throw RequestHandlingException(CommonError.InvalidParameterValue(ListingParamNames.Sort.SORT_ORDER))
    }

    return ListingQueryParams(
        pageParams = PageParams(page = pageNumber, size = pageSize),
        sortBy = sortBy,
        sortOrder = sortOrder
    )
}
