package io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.storage

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFilePrivate
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetFileUseCase @Inject constructor(
    private val storageManager: StorageManager
) {
    /**
     * Retrieves detailed private view of a specific file uploaded by the current authenticated user.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE].
     *
     * **Workflow:**
     * 1. Validates the existence and accessibility of the user via [UserManager].
     * 2. Verifies that the account status permits data retrieval.
     * 3. Fetches the file details via [StorageManager.getSelfStorageFilePrivate].
     * 4. Ensures the file exists and belongs to the caller.
     *
     * @param fileId The unique identifier of the requested file.
     * @param authenticatedRequestContext The context of the authenticated request.
     * @return [AppResult] containing the [StorageFilePrivate] or a [CommonError.NotFound] error.
     */
    suspend operator fun invoke(
        fileId: FileId,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<StorageFilePrivate> {
        val currentUserId = authenticatedRequestContext.userId

        return storageManager.getSelfStorageFilePrivate(
            ownerUserId = currentUserId.asHexDashString(),
            fileId = fileId
        ).mapNotNullOrError(
            CommonError.NotFound(
                resource = StorageFilePrivate::class.java.simpleName,
                identifier = fileId.asHexDashString()
            )
        )
    }
}