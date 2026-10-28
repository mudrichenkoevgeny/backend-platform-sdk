package io.github.mudrichenkoevgeny.backend.core.storage.database.repository

import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFileInternal
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.listing.StorageSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.status.StorageFileStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility
import kotlin.time.Instant

/**
 * Data access for storage files (persistence and querying).
 *
 * Implementations are responsible for storing file records and returning paginated lists
 * with optional filters.
 */
interface StorageFilesRepository {

    /**
     * Persists a storage file metadata record.
     *
     * @param storageFile internal file model to create or update.
     * @return [AppResult] containing the saved [StorageFileInternal] or an error.
     */
    suspend fun createStorageFile(storageFile: StorageFileInternal): AppResult<StorageFileInternal>

    /**
     * Deletes a file record by the unique file identifier.
     *
     * @param fileId unique file identifier.
     * @return [AppResult] containing [Unit] on success or an error.
     */
    suspend fun deleteStorageFile(fileId: FileId): AppResult<Unit>

    /**
     * Soft deletes a file record by the unique file identifier, optionally scoped to an owner.
     *
     * @param fileId Unique file identifier.
     * @param ownerUserId Optional owner user ID to ensure only the user's file is deleted.
     * @return [AppResult] containing [Unit] on success or an error.
     */
    suspend fun deleteStorageFile(fileId: FileId, ownerUserId: String? = null): AppResult<Unit>

    /**
     * Deletes multiple file records in a single batch operation by their unique file identifiers.
     *
     * @param fileIds list of unique file identifiers.
     * @return [AppResult] containing [Unit] on success or an error.
     */
    suspend fun deleteStorageFilesBatch(fileIds: List<FileId>): AppResult<Unit>

    /**
     * Deletes multiple file records in a single batch operation.
     * Optionally filters by owner.
     *
     * @param fileIds List of unique file identifiers.
     * @param ownerUserId Optional owner user ID to ensure only the user's files are deleted.
     * @return [AppResult] containing [Unit] on success or an error.
     */
    suspend fun deleteStorageFilesBatch(fileIds: List<FileId>, ownerUserId: String? = null): AppResult<Unit>

    /**
     * Updates storage file fields.
     *
     * @param fileId unique file identifier.
     * @param status new file status.
     * @param fileSizeBytes actual uploaded file size.
     * @param contentHash optional hash of the content.
     * @return [AppResult] containing the updated [StorageFileInternal].
     */
    suspend fun updateStorageFile(
        fileId: FileId,
        status: UpdateField<StorageFileStatus> = UpdateField.Ignore,
        fileSizeBytes: UpdateField<Long> = UpdateField.Ignore,
        contentHash: UpdateField<String?> = UpdateField.Ignore
    ): AppResult<StorageFileInternal>

    /**
     * Counts active unfinalized upload records for a given user.
     *
     * @param ownerUserId unique owner user identifier string.
     * @return [AppResult] containing the count of pending uploads or an error.
     */
    suspend fun countPendingUploadsForUser(ownerUserId: String): AppResult<Long>

    /**
     * Calculates the cumulative total file size in bytes for active files owned by a user.
     *
     * @param ownerUserId unique owner user identifier string.
     * @return [AppResult] containing used storage size in bytes or an error.
     */
    suspend fun calculateUsedStorageForUser(ownerUserId: String): AppResult<Long>

    /**
     * Retrieves a file record by its unique identifier.
     *
     * @param fileId unique file identifier.
     * @return [AppResult] containing [StorageFileInternal] when found, `null` when missing, or an error.
     */
    suspend fun getStorageFileById(fileId: FileId): AppResult<StorageFileInternal?>

    /**
     * Retrieves a paginated list of storage files for management purposes.
     *
     * @param pageParams Pagination settings.
     * @param sortBy Field to sort the results by.
     * @param sortOrder Sorting direction.
     * @param ownerUserIds Optional filter for specific owner user IDs.
     * @param categories Optional filter by categories.
     * @param visibilities Optional filter by visibilities.
     * @param statuses Optional filter by file statuses.
     * @param mimeTypes Optional filter by MIME type substrings.
     * @param fileNames Optional filter by file name substrings.
     * @return [AppResult] containing [PagedResult] of [StorageFileInternal] records or an error.
     */
    suspend fun getStorageFilesPageForManagement(
        pageParams: PageParams,
        sortBy: StorageSortValues.StorageFileSortBy,
        sortOrder: SortOrder,
        ownerUserIds: List<String>,
        categories: List<StorageCategory>,
        visibilities: List<StorageFileVisibility>,
        statuses: List<StorageFileStatus>,
        mimeTypes: List<String>,
        fileNames: List<String>
    ): AppResult<PagedResult<StorageFileInternal>>

    /**
     * Retrieves a paginated list of storage files belonging to a specific user.
     *
     * @param ownerUserId The ID of the owner.
     * @param pageParams Pagination settings.
     * @param sortBy Field to sort the results by.
     * @param sortOrder Sorting direction.
     * @param categories Optional filter by categories.
     * @param visibilities Optional filter by visibilities.
     * @param statuses Optional filter by file statuses.
     * @param mimeTypes Optional filter by MIME type substrings.
     * @param fileNames Optional filter by file name substrings.
     * @return [AppResult] containing [PagedResult] of [StorageFileInternal] records or an error.
     */
    suspend fun getStorageFilesPageByOwnerUserId(
        ownerUserId: String,
        pageParams: PageParams,
        sortBy: StorageSortValues.StorageFileSortBy,
        sortOrder: SortOrder,
        categories: List<StorageCategory>,
        visibilities: List<StorageFileVisibility>,
        statuses: List<StorageFileStatus>,
        mimeTypes: List<String>,
        fileNames: List<String>
    ): AppResult<PagedResult<StorageFileInternal>>

    /**
     * Retrieves a list of storage file records filtered by status.
     *
     * @param status File status filter.
     * @param limit Maximum number of records to return.
     * @return [AppResult] containing list of [StorageFileInternal] records or an error.
     */
    suspend fun getStorageFilesByStatus(
        status: StorageFileStatus,
        limit: Int
    ): AppResult<List<StorageFileInternal>>

    /**
     * Hard deletes storage file records from the database permanently by their unique file identifiers.
     *
     * @param fileIds List of unique file identifiers to delete.
     * @return [AppResult] containing [Unit] on success or an error.
     */
    suspend fun hardDeleteStorageFilesBatch(fileIds: List<FileId>): AppResult<Unit>

    /**
     * Retrieves a list of storage file records in PENDING_UPLOAD status created before the specified timestamp.
     *
     * @param createdBefore Threshold creation timestamp.
     * @param limit Maximum number of records to return.
     * @return [AppResult] containing list of [StorageFileInternal] records or an error.
     */
    suspend fun getExpiredPendingUploadFiles(
        createdBefore: Instant,
        limit: Int
    ): AppResult<List<StorageFileInternal>>

    /**
     * Batch updates metadata and status for a list of storage file records.
     *
     * @param files List of [StorageFileInternal] records to update.
     * @return [AppResult] containing [Unit] on success or an error.
     */
    suspend fun updateStorageFilesBatch(files: List<StorageFileInternal>): AppResult<Unit>
}