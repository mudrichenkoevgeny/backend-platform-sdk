package io.github.mudrichenkoevgeny.backend.core.storage.network.query

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.getQueryValues
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseListingQueryParams
import io.github.mudrichenkoevgeny.backend.core.storage.network.model.ManagementStorageFileListQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.listing.StorageFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.listing.StorageSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.status.StorageFileStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility
import io.ktor.server.application.ApplicationCall

fun ApplicationCall.parseManagementStorageFileListQueryParams(): ManagementStorageFileListQueryParams {
    val listing = parseListingQueryParams(
        defaultSortBy = StorageSortValues.StorageFileSortBy.DEFAULT,
        parseSortByOrNull = StorageSortValues.StorageFileSortBy::fromValueOrNull
    )
    val filterNames = StorageFilterValues.StorageFileFilterValues

    val categories = getQueryValues(filterNames.CATEGORY).map { category ->
        StorageCategory.fromValueOrNull(category)
            ?: throw RequestHandlingException(CommonError.InvalidParameterValue(filterNames.CATEGORY))
    }

    val visibilities = getQueryValues(filterNames.VISIBILITY).map { visibility ->
        StorageFileVisibility.fromValueOrNull(visibility)
            ?: throw RequestHandlingException(CommonError.InvalidParameterValue(filterNames.VISIBILITY))
    }

    val statuses = getQueryValues(filterNames.STATUS).map { status ->
        StorageFileStatus.fromValueOrNull(status)
            ?: throw RequestHandlingException(CommonError.InvalidParameterValue(filterNames.STATUS))
    }

    return ManagementStorageFileListQueryParams(
        listing = listing,
        ownerUserIds = getQueryValues(filterNames.OWNER_USER_ID),
        categories = categories,
        visibilities = visibilities,
        statuses = statuses,
        mimeTypes = getQueryValues(filterNames.MIME_TYPE),
        fileNames = getQueryValues(filterNames.FILE_NAME)
    )
}
