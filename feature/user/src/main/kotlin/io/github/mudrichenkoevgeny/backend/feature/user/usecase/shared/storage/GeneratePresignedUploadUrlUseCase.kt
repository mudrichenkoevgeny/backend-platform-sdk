package io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.storage

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.presigned.PresignedUrl
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for initiating a file upload.
 *
 * Delegates quota validation, metadata creation, and presigned URL generation to [StorageManager].
 *
 * @property userManager Manager for retrieving current user profile and custom storage limits.
 * @property storageManager Manager for storage file metadata and quotas.
 */
@Singleton
class GeneratePresignedUploadUrlUseCase @Inject constructor(
    private val userManager: UserManager,
    private val storageManager: StorageManager
) {

    /**
     * Executes upload initialization.
     *
     * @param fileName Original file name.
     * @param fileSizeBytes Size of the file in bytes.
     * @param mimeType MIME type of the file.
     * @param storageCategory Storage category domain enum.
     * @param storageFileVisibility Storage visibility domain enum.
     * @param targetScope Optional scope identifier.
     * @param authenticatedRequestContext Context of the requesting authenticated user.
     * @return [AppResult.Success] containing [PresignedUrl].
     */
    suspend operator fun invoke(
        fileName: String,
        fileSizeBytes: Long,
        mimeType: String,
        storageCategory: StorageCategory,
        storageFileVisibility: StorageFileVisibility,
        targetScope: String? = null,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<PresignedUrl> {
        val currentUserId = authenticatedRequestContext.userId

        val getCurrentUserResult = userManager.getUserInternalById(currentUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val currentUser = when (getCurrentUserResult) {
            is AppResult.Error -> return getCurrentUserResult
            is AppResult.Success -> getCurrentUserResult.data
        }

        val isManagementUser = currentUser.role in setOf(UserRole.STAFF, UserRole.ADMIN)

        return storageManager.initiateFileUpload(
            ownerUserId = currentUserId.asHexDashString(),
            isManagementUser = isManagementUser,
            fileName = fileName,
            fileSizeBytes = fileSizeBytes,
            mimeType = mimeType,
            category = storageCategory,
            visibility = storageFileVisibility,
            targetScope = targetScope,
            customStorageQuotaBytes = currentUser.storageConfiguration?.quotaBytes,
            customMaxUploadSizeBytes = currentUser.storageConfiguration?.maxUploadSizeBytes,
            customMaxPendingUploads = currentUser.storageConfiguration?.maxPendingUploads
        )
    }
}