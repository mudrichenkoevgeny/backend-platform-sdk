package io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.storage

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFileSummary
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.listing.StorageSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.status.StorageFileStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetFilesUseCase @Inject constructor(
    private val storageManager: StorageManager
) {
    /**
     * Retrieves a paginated list of summary storage files uploaded by the current authenticated user.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE].
     *
     * **Workflow:**
     * 1. Validates the existence of the caller via [UserManager].
     * 2. Verifies that the account status permits data retrieval.
     * 3. Fetches a filtered and sorted page of files via [StorageManager.getSelfStorageFilesSummaryPage].
     *
     * @param authenticatedRequestContext Context containing the authenticated user's ID.
     * @param pageParams Pagination settings (page index and size).
     * @param sortBy Field to sort by (e.g., created at, file size).
     * @param sortOrder Sorting direction (ASC/DESC).
     * @param categories Optional filter by storage categories.
     * @param visibilities Optional filter by file visibilities.
     * @param statuses Optional filter by file statuses.
     * @param mimeTypes Optional filter by MIME type substrings.
     * @param fileNames Optional filter by file name substrings.
     * @return [AppResult] with [PagedResult] of [StorageFileSummary].
     */
    suspend operator fun invoke(
        authenticatedRequestContext: AuthenticatedRequestContext,
        pageParams: PageParams = PageParams(),
        sortBy: StorageSortValues.StorageFileSortBy = StorageSortValues.StorageFileSortBy.DEFAULT,
        sortOrder: SortOrder = SortOrder.DEFAULT,
        categories: List<StorageCategory> = emptyList(),
        visibilities: List<StorageFileVisibility> = emptyList(),
        statuses: List<StorageFileStatus> = emptyList(),
        mimeTypes: List<String> = emptyList(),
        fileNames: List<String> = emptyList()
    ): AppResult<PagedResult<StorageFileSummary>> {
        val currentUserId = authenticatedRequestContext.userId

        return storageManager.getSelfStorageFilesSummaryPage(
            ownerUserId = currentUserId.asHexDashString(),
            pageParams = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            categories = categories,
            visibilities = visibilities,
            statuses = statuses,
            mimeTypes = mimeTypes,
            fileNames = fileNames
        )
    }
}