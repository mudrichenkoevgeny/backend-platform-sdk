package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storage

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFileSummary
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.listing.StorageSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.status.StorageFileStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.permission.StoragePermissionCode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for administrative retrieval of paginated storage file summaries across the platform.
 *
 * Enforces account status and permission checks via [UserManager]
 * before fetching storage records via [StorageManager].
 *
 * @property storageManager Manager for storage file operations.
 * @property userManager Manager for retrieving user account status and permissions.
 */
@Singleton
class ManagementGetFilesUseCase @Inject constructor(
    private val storageManager: StorageManager,
    private val userManager: UserManager
) {

    /**
     * Retrieves a paginated list of all storage files across the platform for administrative inspection.
     *
     * @param authenticatedRequestContext Context containing the authenticated user's ID.
     * @param pageParams Pagination settings (page index and size).
     * @param sortBy Field to sort the results by.
     * @param sortOrder Sorting direction (ASC/DESC).
     * @param ownerUserIds Optional filter for specific owner user IDs.
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
        ownerUserIds: List<String> = emptyList(),
        categories: List<StorageCategory> = emptyList(),
        visibilities: List<StorageFileVisibility> = emptyList(),
        statuses: List<StorageFileStatus> = emptyList(),
        mimeTypes: List<String> = emptyList(),
        fileNames: List<String> = emptyList()
    ): AppResult<PagedResult<StorageFileSummary>> {
        val managementUserId = authenticatedRequestContext.userId

        val managementUserResult = userManager.getUserInternalById(managementUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val managementUser = when (managementUserResult) {
            is AppResult.Success -> managementUserResult.data
            is AppResult.Error -> return managementUserResult
        }

        if (StoragePermissionCode.STORAGE_VIEW_OTHER_USERS_FILES !in managementUser.permissionCodes) {
            return AppResult.Error(UserError.UserMissingPermissions(managementUserId))
        }

        return storageManager.getStorageFilesSummaryPage(
            managementUserPermissionCodes = managementUser.permissionCodes,
            pageParams = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            ownerUserIds = ownerUserIds,
            categories = categories,
            visibilities = visibilities,
            statuses = statuses,
            mimeTypes = mimeTypes,
            fileNames = fileNames
        )
    }
}