package io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.storage

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFilePrivate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for finalizing an upload session after client uploads binary to S3.
 *
 * Delegates status verification, S3 metadata validation, and database updates to [StorageManager].
 *
 * @property storageManager Manager for storage file metadata and status updates.
 */
@Singleton
class ConfirmUploadUseCase @Inject constructor(
    private val storageManager: StorageManager
) {

    /**
     * Executes upload confirmation.
     *
     * @param fileId Target file ID.
     * @param authenticatedRequestContext Context of the requesting authenticated user.
     * @return [AppResult] containing updated [StorageFilePrivate] on success or an error.
     */
    suspend operator fun invoke(
        fileId: FileId,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<StorageFilePrivate> {
        val ownerUserId = authenticatedRequestContext.userId.asHexDashString()
        return storageManager.confirmUpload(
            ownerUserId = ownerUserId,
            fileId = fileId
        )
    }
}