package io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.storage

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for self-service batch deletion of user files.
 *
 * Validates file ownership implicitly at the database level and performs a soft delete.
 * Physical deletion from S3 is handled asynchronously by a background garbage collection job.
 *
 * @property storageManager Manager for storage file metadata and soft deletion.
 */
@Singleton
class DeleteFilesUseCase @Inject constructor(
    private val storageManager: StorageManager
) {

    /**
     * Executes self-service batch file deletion.
     *
     * @param fileIds List of target file IDs.
     * @param authenticatedRequestContext Context containing the authenticated user's ID.
     * @return [AppResult.Success] on completion, or an error on failure.
     */
    suspend operator fun invoke(
        fileIds: List<FileId>,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<Unit> {
        if (fileIds.isEmpty()) {
            return AppResult.Success(Unit)
        }

        val ownerUserId = authenticatedRequestContext.userId.asHexDashString()
        return storageManager.deleteSelfStorageFilesBatch(
            ownerUserId = ownerUserId,
            fileIds = fileIds
        )
    }
}