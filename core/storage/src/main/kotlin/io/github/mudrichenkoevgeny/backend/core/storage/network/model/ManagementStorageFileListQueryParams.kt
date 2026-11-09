package io.github.mudrichenkoevgeny.backend.core.storage.network.model

import io.github.mudrichenkoevgeny.backend.core.common.pagination.ListingQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.listing.StorageFileSortBy
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.status.StorageFileStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility
import kotlin.time.Instant

/**
 * Query parameters for filtering and paginating storage files in management API.
 */
public data class ManagementStorageFileListQueryParams(
    val listing: ListingQueryParams<StorageFileSortBy>,
    val fileIds: List<FileId> = emptyList(),
    val ownerUserIds: List<String> = emptyList(),
    val isOwnerUserIdSet: Boolean? = null,
    val fileName: String? = null,
    val fileSizeBytesFrom: Long? = null,
    val fileSizeBytesTo: Long? = null,
    val mimeTypes: List<String> = emptyList(),
    val categories: List<StorageCategory> = emptyList(),
    val excludeCategories: List<StorageCategory> = emptyList(),
    val visibilities: List<StorageFileVisibility> = emptyList(),
    val excludeVisibilities: List<StorageFileVisibility> = emptyList(),
    val statuses: List<StorageFileStatus> = emptyList(),
    val excludeStatuses: List<StorageFileStatus> = listOf(StorageFileStatus.DELETED),
    val targetScopes: List<String> = emptyList(),
    val isTargetScopeSet: Boolean? = null,
    val createdAtFrom: Instant? = null,
    val createdAtTo: Instant? = null,
    val updatedAtFrom: Instant? = null,
    val updatedAtTo: Instant? = null,
    val isUpdatedAtSet: Boolean? = null,
    val searchQuery: String? = null
)
