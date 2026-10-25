package io.github.mudrichenkoevgeny.backend.core.storage.network.model

import io.github.mudrichenkoevgeny.backend.core.common.pagination.ListingQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.listing.StorageSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.status.StorageFileStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility

data class SelfStorageFileListQueryParams(
    val listing: ListingQueryParams<StorageSortValues.StorageFileSortBy>,
    val categories: List<StorageCategory>,
    val visibilities: List<StorageFileVisibility>,
    val statuses: List<StorageFileStatus>,
    val mimeTypes: List<String>,
    val fileNames: List<String>
)
