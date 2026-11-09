package io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.storage

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.core.storage.network.model.SelfStorageFileListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFileSummary
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to retrieve user storage files page.
 */
@Singleton
public class GetFilesUseCase @Inject constructor(
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
     * @param queryParams Structured filter and pagination parameters ([SelfStorageFileListQueryParams]).
     * @return [AppResult] with [PagedResult] of [StorageFileSummary].
     */
    public suspend operator fun invoke(
        authenticatedRequestContext: AuthenticatedRequestContext,
        queryParams: SelfStorageFileListQueryParams
    ): AppResult<PagedResult<StorageFileSummary>> {
        val currentUserId = authenticatedRequestContext.userId

        return storageManager.getSelfStorageFilesSummaryPage(
            ownerUserId = currentUserId.asHexDashString(),
            queryParams = queryParams
        )
    }
}
