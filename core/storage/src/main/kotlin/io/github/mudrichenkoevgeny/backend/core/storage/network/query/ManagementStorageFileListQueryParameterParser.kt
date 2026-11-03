package io.github.mudrichenkoevgeny.backend.core.storage.network.query

import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.firstNonBlankQueryValue
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.getQueryValues
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseBooleanQueryOrNull
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseLongQueryOrNull
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseTimestampQueryOrNull
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseValidatedQueryList
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseListingQueryParams
import io.github.mudrichenkoevgeny.backend.core.storage.network.model.ManagementStorageFileListQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.toFileIdOrNull
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.listing.StorageFileFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.listing.StorageFileSortBy
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.status.StorageFileStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility
import io.ktor.server.application.ApplicationCall

/**
 * Parses storage file management list query parameters with strict validation.
 */
fun ApplicationCall.parseManagementStorageFileListQueryParams(): ManagementStorageFileListQueryParams {
    val filterNames = StorageFileFilterValues

    return ManagementStorageFileListQueryParams(
        listing = parseListingQueryParams(
            defaultSortBy = StorageFileSortBy.DEFAULT,
            parseSortByOrNull = StorageFileSortBy::fromValueOrNull
        ),
        fileIds = parseValidatedQueryList(filterNames.FILE_ID, String::toFileIdOrNull),
        ownerUserIds = getQueryValues(filterNames.OWNER_USER_ID),
        isOwnerUserIdSet = parseBooleanQueryOrNull(filterNames.IS_OWNER_USER_ID_SET),
        fileName = firstNonBlankQueryValue(filterNames.FILE_NAME),
        fileSizeBytesFrom = parseLongQueryOrNull(filterNames.FILE_SIZE_BYTES_FROM),
        fileSizeBytesTo = parseLongQueryOrNull(filterNames.FILE_SIZE_BYTES_TO),
        mimeTypes = getQueryValues(filterNames.MIME_TYPE),
        categories = parseValidatedQueryList(filterNames.CATEGORY, StorageCategory::fromValueOrNull),
        excludeCategories = parseValidatedQueryList(filterNames.EXCLUDE_CATEGORY, StorageCategory::fromValueOrNull),
        visibilities = parseValidatedQueryList(filterNames.VISIBILITY, StorageFileVisibility::fromValueOrNull),
        excludeVisibilities = parseValidatedQueryList(filterNames.EXCLUDE_VISIBILITY, StorageFileVisibility::fromValueOrNull),
        statuses = parseValidatedQueryList(filterNames.STATUS, StorageFileStatus::fromValueOrNull),
        excludeStatuses = parseValidatedQueryList(filterNames.EXCLUDE_STATUS, StorageFileStatus::fromValueOrNull).ifEmpty { listOf(StorageFileStatus.DELETED) },
        targetScopes = getQueryValues(filterNames.TARGET_SCOPE),
        isTargetScopeSet = parseBooleanQueryOrNull(filterNames.IS_TARGET_SCOPE_SET),
        createdAtFrom = parseTimestampQueryOrNull(filterNames.CREATED_AT_FROM),
        createdAtTo = parseTimestampQueryOrNull(filterNames.CREATED_AT_TO),
        updatedAtFrom = parseTimestampQueryOrNull(filterNames.UPDATED_AT_FROM),
        updatedAtTo = parseTimestampQueryOrNull(filterNames.UPDATED_AT_TO),
        isUpdatedAtSet = parseBooleanQueryOrNull(filterNames.IS_UPDATED_AT_SET),
        searchQuery = firstNonBlankQueryValue(filterNames.SEARCH_QUERY)
    )
}
