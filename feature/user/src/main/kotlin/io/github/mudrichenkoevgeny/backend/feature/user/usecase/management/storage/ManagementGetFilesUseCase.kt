package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storage

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.core.storage.network.model.ManagementStorageFileListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFileSummary
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.permission.StoragePermissionCode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Management use case for querying paginated and filtered storage file records across the platform.
 *
 * Enforces permission checks (`STORAGE_VIEW_OTHER_USERS_FILES`) before retrieving storage summaries.
 */
@Singleton
public class ManagementGetFilesUseCase @Inject constructor(
    private val storageManager: StorageManager,
    private val userManager: UserManager
) {
    /**
     * Retrieves a paginated list of all storage files across the platform for administrative inspection.
     *
     * @param authenticatedRequestContext Context containing the authenticated user's ID.
     * @param queryParams Structured filter and pagination parameters ([ManagementStorageFileListQueryParams]).
     * @return [AppResult] with [PagedResult] of [StorageFileSummary].
     */
    public suspend operator fun invoke(
        authenticatedRequestContext: AuthenticatedRequestContext,
        queryParams: ManagementStorageFileListQueryParams
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
            queryParams = queryParams
        )
    }
}
