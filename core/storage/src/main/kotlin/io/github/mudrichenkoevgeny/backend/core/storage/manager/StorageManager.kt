package io.github.mudrichenkoevgeny.backend.core.storage.manager

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFilePrivate
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFileSummary
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.listing.StorageSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.presigned.PresignedUrl
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.status.StorageFileStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility

/**
 * Manages storage files operations, mapping internal repositories into domain models.
 */
interface StorageManager {

    /**
     * Validates quotas, creates a pending storage file record, and generates a presigned upload URL.
     *
     * @param ownerUserId Owner user ID string.
     * @param isManagementUser Whether the user has management privileges (affects quotas).
     * @param fileName Original file name.
     * @param fileSizeBytes File size in bytes.
     * @param mimeType MIME type string.
     * @param category Storage category enum.
     * @param visibility Storage visibility enum.
     * @param targetScope Optional target scope.
     * @param customStorageQuotaBytes Optional custom storage quota override for the user. If null, the global setting is used.
     * @param customMaxUploadSizeBytes Optional custom max upload size override for the user. If null, the global setting is used.
     * @param customMaxPendingUploads Optional custom max pending uploads override for the user. If null, the global setting is used.
     * @return [AppResult] containing domain [PresignedUrl] or an error.
     */
    suspend fun initiateFileUpload(
        ownerUserId: String,
        isManagementUser: Boolean,
        fileName: String,
        fileSizeBytes: Long,
        mimeType: String,
        category: StorageCategory,
        visibility: StorageFileVisibility,
        targetScope: String?,
        customStorageQuotaBytes: Long? = null,
        customMaxUploadSizeBytes: Long? = null,
        customMaxPendingUploads: Int? = null
    ): AppResult<PresignedUrl>

    /**
     * Confirms upload by verifying S3 object size and marks file as READY.
     *
     * @param ownerUserId Owner user ID string.
     * @param fileId Target pending file ID.
     * @return [AppResult] containing updated [StorageFilePrivate] or an error.
     */
    suspend fun confirmUpload(
        ownerUserId: String,
        fileId: FileId
    ): AppResult<StorageFilePrivate>

    /**
     * Deletes a file record by the unique file identifier.
     *
     * @param fileId Unique file identifier.
     * @return [AppResult] containing [Unit] on success or an error.
     */
    suspend fun deleteStorageFile(fileId: FileId): AppResult<Unit>

    /**
     * Deletes a file record belonging to a specific user by its unique identifier.
     *
     * @param ownerUserId Owner user ID string.
     * @param fileId Unique file identifier.
     * @return [AppResult] containing [Unit] on success or an error.
     */
    suspend fun deleteSelfStorageFile(ownerUserId: String, fileId: FileId): AppResult<Unit>

    /**
     * Deletes multiple file records unconditionally in a single batch operation.
     *
     * @param fileIds List of unique file identifiers.
     * @return [AppResult] containing [Unit] on success or an error.
     */
    suspend fun deleteStorageFilesBatch(fileIds: List<FileId>): AppResult<Unit>

    /**
     * Deletes multiple file records belonging to a specific user in a single batch operation.
     *
     * @param ownerUserId Owner user ID string.
     * @param fileIds List of unique file identifiers.
     * @return [AppResult] containing [Unit] on success or an error.
     */
    suspend fun deleteSelfStorageFilesBatch(ownerUserId: String, fileIds: List<FileId>): AppResult<Unit>

    /**
     * Retrieves a detailed private view of a specific file record owned by the requesting user.
     *
     * Designed for "Self" service scenarios.
     *
     * @param ownerUserId The ID of the owner requesting their own file.
     * @param fileId Target file ID.
     * @return [AppResult.Success] containing [StorageFilePrivate], null if missing, or an error.
     */
    suspend fun getSelfStorageFilePrivate(
        ownerUserId: String,
        fileId: FileId
    ): AppResult<StorageFilePrivate?>

    /**
     * Retrieves a detailed private view of a specific file record for management roles.
     *
     * Applies permission-aware access checks based on [managementUserPermissionCodes].
     *
     * @param managementUserPermissionCodes Caller permissions used to derive access rules.
     * @param fileId Target file ID.
     * @return [AppResult.Success] containing [StorageFilePrivate], null if missing, or an error.
     */
    suspend fun getStorageFilePrivate(
        managementUserPermissionCodes: Set<PermissionCode>,
        fileId: FileId
    ): AppResult<StorageFilePrivate?>

    /**
     * Returns a paginated list of summary storage files uploaded by the current user.
     *
     * Designed for "Self" service scenarios.
     *
     * @param ownerUserId The ID of the owner requesting their own files.
     * @param pageParams Pagination settings (page index and size).
     * @param sortBy Field to sort the results by.
     * @param sortOrder Sorting direction (ASC/DESC).
     * @param categories Optional filter by categories.
     * @param visibilities Optional filter by visibilities.
     * @param statuses Optional filter by file statuses.
     * @param mimeTypes Optional filter by MIME type substrings.
     * @param fileNames Optional filter by file name substrings.
     * @return [AppResult.Success] with [PagedResult] of the user's storage file summaries or an error.
     */
    suspend fun getSelfStorageFilesSummaryPage(
        ownerUserId: String,
        pageParams: PageParams,
        sortBy: StorageSortValues.StorageFileSortBy = StorageSortValues.StorageFileSortBy.DEFAULT,
        sortOrder: SortOrder = SortOrder.DEFAULT,
        categories: List<StorageCategory> = emptyList(),
        visibilities: List<StorageFileVisibility> = emptyList(),
        statuses: List<StorageFileStatus> = emptyList(),
        mimeTypes: List<String> = emptyList(),
        fileNames: List<String> = emptyList()
    ): AppResult<PagedResult<StorageFileSummary>>

    /**
     * Returns a paginated list of all storage files across the platform for administrative inspection.
     *
     * Applies role-scoped visibility based on [managementUserPermissionCodes].
     *
     * @param managementUserPermissionCodes Caller permissions used to derive access rules.
     * @param pageParams Pagination settings (page index and size).
     * @param sortBy Field to sort the results by.
     * @param sortOrder Sorting direction (ASC/DESC).
     * @param ownerUserIds Optional filter for specific owner user IDs.
     * @param categories Optional filter by categories.
     * @param visibilities Optional filter by visibilities.
     * @param statuses Optional filter by file statuses.
     * @param mimeTypes Optional filter by MIME type substrings.
     * @param fileNames Optional filter by file name substrings.
     * @return [AppResult.Success] with [PagedResult] of storage file summaries or an error.
     */
    suspend fun getStorageFilesSummaryPage(
        managementUserPermissionCodes: Set<PermissionCode>,
        pageParams: PageParams,
        sortBy: StorageSortValues.StorageFileSortBy = StorageSortValues.StorageFileSortBy.DEFAULT,
        sortOrder: SortOrder = SortOrder.DEFAULT,
        ownerUserIds: List<String> = emptyList(),
        categories: List<StorageCategory> = emptyList(),
        visibilities: List<StorageFileVisibility> = emptyList(),
        statuses: List<StorageFileStatus> = emptyList(),
        mimeTypes: List<String> = emptyList(),
        fileNames: List<String> = emptyList()
    ): AppResult<PagedResult<StorageFileSummary>>
}