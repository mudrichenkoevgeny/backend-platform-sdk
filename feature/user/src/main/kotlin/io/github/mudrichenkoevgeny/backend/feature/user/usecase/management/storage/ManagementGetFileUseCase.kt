package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storage

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFilePrivate
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.permission.StoragePermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for administrative retrieval of detailed file metadata.
 *
 * Enforces account status and permission checks via [UserManager]
 * before delegating file resolution to [StorageManager].
 *
 * @property storageManager Manager for storage file operations.
 * @property userManager Manager for retrieving user account status and permissions.
 */
@Singleton
class ManagementGetFileUseCase @Inject constructor(
    private val storageManager: StorageManager,
    private val userManager: UserManager
) {

    /**
     * Retrieves detailed private view of a specific file record for management roles.
     *
     * @param fileId Target file ID.
     * @param authenticatedRequestContext Context containing the authenticated user's ID.
     * @return [AppResult] containing [StorageFilePrivate] or an error.
     */
    suspend operator fun invoke(
        fileId: FileId,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<StorageFilePrivate> {
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

        return storageManager.getStorageFilePrivate(
            managementUserPermissionCodes = managementUser.permissionCodes,
            fileId = fileId
        ).mapNotNullOrError(
            CommonError.NotFound(
                resource = StorageFilePrivate::class.java.simpleName,
                identifier = fileId.asHexDashString()
            )
        )
    }
}