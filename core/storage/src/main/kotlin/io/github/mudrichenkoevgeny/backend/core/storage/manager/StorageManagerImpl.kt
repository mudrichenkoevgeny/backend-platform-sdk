package io.github.mudrichenkoevgeny.backend.core.storage.manager

import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.dataOrNull
import io.github.mudrichenkoevgeny.backend.core.common.result.flatMapSuccess
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.common.result.mapSuccess
import io.github.mudrichenkoevgeny.backend.core.database.util.dbQuery
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig
import io.github.mudrichenkoevgeny.backend.core.storage.database.repository.StorageFilesRepository
import io.github.mudrichenkoevgeny.backend.core.storage.error.model.StorageError
import io.github.mudrichenkoevgeny.backend.core.storage.service.StorageService
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.common.mapper.pagedresult.mapItems
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFileInternal
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFilePrivate
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFileSummary
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.listing.StorageSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.presigned.PresignedUrl
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.status.StorageFileStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.permission.StoragePermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.mapper.file.toStorageFilePrivate
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.mapper.file.toStorageFileSummary
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Default [StorageManager] implementation backed by [StorageFilesRepository] wrapped in [dbQuery].
 */
@Singleton
class StorageManagerImpl @Inject constructor(
    private val filesRepository: StorageFilesRepository,
    private val settingsProvider: StorageSettingsProvider,
    private val storageService: StorageService,
    private val storageConfig: StorageConfig
) : StorageManager {

    override suspend fun initiateFileUpload(
        ownerUserId: String,
        isManagementUser: Boolean,
        fileName: String,
        fileSizeBytes: Long,
        mimeType: String,
        category: StorageCategory,
        visibility: StorageFileVisibility,
        targetScope: String?,
        customStorageQuotaBytes: Long?,
        customMaxUploadSizeBytes: Long?,
        customMaxPendingUploads: Int?
    ): AppResult<PresignedUrl> = dbQuery {
        val minUploadSizeBytes = settingsProvider.getMinUploadSizeBytes()
        if (fileSizeBytes < minUploadSizeBytes) {
            return@dbQuery AppResult.Error(
                StorageError.FileSizeBelowMinimum(
                    fileSizeBytes = fileSizeBytes,
                    minFileSizeBytes = minUploadSizeBytes,
                    category = category.value,
                    fileName = fileName
                )
            )
        }

        val maxUploadSizeBytes = customMaxUploadSizeBytes ?: settingsProvider.getMaxUploadSizeBytes()
        if (fileSizeBytes > maxUploadSizeBytes) {
            return@dbQuery AppResult.Error(
                StorageError.FileSizeExceedsLimit(
                    fileSizeBytes = fileSizeBytes,
                    maxFileSizeBytes = maxUploadSizeBytes,
                    category = category.value,
                    fileName = fileName
                )
            )
        }

        val pendingCountResult = filesRepository.countPendingUploadsForUser(ownerUserId)
        val pendingCount = when (pendingCountResult) {
            is AppResult.Success -> pendingCountResult.data
            is AppResult.Error -> return@dbQuery pendingCountResult
        }

        val maxPending = customMaxPendingUploads ?: if (isManagementUser) {
            settingsProvider.getMaxPendingUploadsForManagementUser()
        } else {
            settingsProvider.getMaxPendingUploadsForOpenUser()
        }

        if (pendingCount >= maxPending) {
            return@dbQuery AppResult.Error(StorageError.TooManyPendingUploads(maxPending))
        }

        val effectiveQuota = customStorageQuotaBytes ?: if (isManagementUser) {
            settingsProvider.getTotalStorageQuotaForManagementUserBytes()
        } else {
            settingsProvider.getTotalStorageQuotaForOpenUserBytes()
        }

        if (effectiveQuota != null) {
            val usedStorageResult = filesRepository.calculateUsedStorageForUser(ownerUserId)
            val usedBytes = when (usedStorageResult) {
                is AppResult.Success -> usedStorageResult.data
                is AppResult.Error -> return@dbQuery usedStorageResult
            }
            if (usedBytes + fileSizeBytes > effectiveQuota) {
                return@dbQuery AppResult.Error(StorageError.StorageQuotaExceeded(effectiveQuota, usedBytes))
            }
        }

        val now = Clock.System.now()
        val fileId = FileId.generate()
        val s3Key = "${now.epochSeconds}/${fileId.value}"

        val expirationSeconds = settingsProvider.getPresignedUrlExpirationSeconds()
        val presignedUrlResult = storageService.generatePresignedPutUrl(
            key = s3Key,
            contentType = mimeType,
            expirationSeconds = expirationSeconds
        )
        val uploadUrl = when (presignedUrlResult) {
            is AppResult.Success -> presignedUrlResult.data
            is AppResult.Error -> return@dbQuery presignedUrlResult
        }

        val urlResult = storageService.getUrl(s3Key)
        val fileUrl = if (urlResult is AppResult.Success) urlResult.data else ""
        val expiresAt = now + expirationSeconds.seconds

        val storageFile = StorageFileInternal(
            id = fileId,
            ownerUserId = ownerUserId,
            fileName = fileName,
            fileSizeBytes = fileSizeBytes,
            mimeType = mimeType,
            category = category,
            visibility = visibility,
            status = StorageFileStatus.PENDING_UPLOAD,
            targetScope = targetScope,
            contentHash = null,
            s3Bucket = storageConfig.s3BucketName,
            s3Key = s3Key,
            createdAt = now,
            updatedAt = now
        )

        val saveResult = filesRepository.createStorageFile(storageFile)
        if (saveResult is AppResult.Error) return@dbQuery saveResult

        AppResult.Success(
            PresignedUrl(
                fileId = storageFile.id,
                uploadUrl = uploadUrl,
                fileUrl = fileUrl,
                expiresAt = expiresAt
            )
        )
    }

    override suspend fun confirmUpload(
        ownerUserId: String,
        fileId: FileId
    ): AppResult<StorageFilePrivate> = dbQuery {
        filesRepository.getStorageFileById(fileId)
            .mapNotNullOrError(StorageError.FileNotFound(fileId))
            .flatMapSuccess { file ->
                if (file.ownerUserId != ownerUserId) {
                    return@flatMapSuccess AppResult.Error(StorageError.FileNotFound(fileId))
                }

                if (file.status != StorageFileStatus.PENDING_UPLOAD) {
                    return@flatMapSuccess AppResult.Error(
                        StorageError.StorageUploadFailed(
                            fileId = fileId,
                            message = "File is not pending upload"
                        )
                    )
                }

                storageService.getObjectMetadata(file.s3Key)
                    .mapNotNullOrError(
                        StorageError.StorageUploadFailed(
                            fileId = fileId,
                            message = "Failed to verify S3 object metadata"
                        )
                    )
                    .flatMapSuccess { metadata ->
                        filesRepository.updateStorageFile(
                            fileId = fileId,
                            status = UpdateField.Set(StorageFileStatus.READY),
                            fileSizeBytes = UpdateField.Set(metadata.sizeBytes),
                            contentHash = UpdateField.Set(metadata.eTag)
                        )
                    }
            }
            .mapSuccess { updatedFile ->
                val fileUrl = storageService.getUrl(updatedFile.s3Key).dataOrNull().orEmpty()
                updatedFile.toStorageFilePrivate(fileUrl = fileUrl)
            }
    }

    override suspend fun deleteStorageFile(fileId: FileId): AppResult<Unit> = dbQuery {
        filesRepository.deleteStorageFile(fileId = fileId, ownerUserId = null)
    }

    override suspend fun deleteSelfStorageFile(ownerUserId: String, fileId: FileId): AppResult<Unit> = dbQuery {
        filesRepository.deleteStorageFile(fileId = fileId, ownerUserId = ownerUserId)
    }

    override suspend fun deleteStorageFilesBatch(fileIds: List<FileId>): AppResult<Unit> = dbQuery {
        if (fileIds.isEmpty()) {
            return@dbQuery AppResult.Success(Unit)
        }
        filesRepository.deleteStorageFilesBatch(fileIds = fileIds, ownerUserId = null)
    }

    override suspend fun deleteSelfStorageFilesBatch(
        ownerUserId: String,
        fileIds: List<FileId>
    ): AppResult<Unit> = dbQuery {
        if (fileIds.isEmpty()) {
            return@dbQuery AppResult.Success(Unit)
        }
        filesRepository.deleteStorageFilesBatch(fileIds = fileIds, ownerUserId = ownerUserId)
    }

    override suspend fun getSelfStorageFilePrivate(
        ownerUserId: String,
        fileId: FileId
    ): AppResult<StorageFilePrivate?> = dbQuery {
        val fileResult = filesRepository.getStorageFileById(fileId)
        when (fileResult) {
            is AppResult.Success -> {
                var file = fileResult.data ?: return@dbQuery AppResult.Success(null)
                if (file.ownerUserId != ownerUserId) {
                    return@dbQuery AppResult.Error(StorageError.FileNotFound(fileId))
                }

                if (file.status == StorageFileStatus.PENDING_UPLOAD) {
                    val metadataResult = storageService.getObjectMetadata(file.s3Key)
                    if (metadataResult is AppResult.Success) {
                        val metadata = metadataResult.data
                        val updateResult = filesRepository.updateStorageFile(
                            fileId = file.id,
                            status = UpdateField.Set(StorageFileStatus.READY),
                            fileSizeBytes = UpdateField.Set(metadata.sizeBytes),
                            contentHash = UpdateField.Set(metadata.eTag)
                        )
                        if (updateResult is AppResult.Success) {
                            file = updateResult.data
                        }
                    }
                }

                val fileUrl = storageService.getUrl(file.s3Key).dataOrNull().orEmpty()
                AppResult.Success(file.toStorageFilePrivate(fileUrl = fileUrl))
            }
            is AppResult.Error -> fileResult
        }
    }

    override suspend fun getStorageFilePrivate(
        managementUserPermissionCodes: Set<PermissionCode>,
        fileId: FileId
    ): AppResult<StorageFilePrivate?> = dbQuery {
        if (!managementUserPermissionCodes.contains(StoragePermissionCode.STORAGE_VIEW_OTHER_USERS_FILES)) {
            return@dbQuery AppResult.Error(StorageError.FileNotFound(null))
        }

        filesRepository.getStorageFileById(fileId).mapSuccess { file ->
            file?.let {
                val fileUrl = storageService.generatePresignedGetUrl(
                    key = it.s3Key,
                    expirationSeconds = settingsProvider.getPresignedUrlExpirationSeconds()
                ).dataOrNull().orEmpty()

                it.toStorageFilePrivate(fileUrl = fileUrl)
            }
        }
    }

    override suspend fun getSelfStorageFilesSummaryPage(
        ownerUserId: String,
        pageParams: PageParams,
        sortBy: StorageSortValues.StorageFileSortBy,
        sortOrder: SortOrder,
        categories: List<StorageCategory>,
        visibilities: List<StorageFileVisibility>,
        statuses: List<StorageFileStatus>,
        mimeTypes: List<String>,
        fileNames: List<String>
    ): AppResult<PagedResult<StorageFileSummary>> = dbQuery {
        val result = filesRepository.getStorageFilesPageByOwnerUserId(
            ownerUserId = ownerUserId,
            pageParams = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            categories = categories,
            visibilities = visibilities,
            statuses = statuses,
            mimeTypes = mimeTypes,
            fileNames = fileNames
        )

        result.mapSuccess { paged ->
            paged.mapItems { fileInternal ->
                val fileUrl = storageService.getUrl(fileInternal.s3Key).dataOrNull().orEmpty()
                fileInternal.toStorageFileSummary(fileUrl = fileUrl)
            }
        }
    }

    override suspend fun getStorageFilesSummaryPage(
        managementUserPermissionCodes: Set<PermissionCode>,
        pageParams: PageParams,
        sortBy: StorageSortValues.StorageFileSortBy,
        sortOrder: SortOrder,
        ownerUserIds: List<String>,
        categories: List<StorageCategory>,
        visibilities: List<StorageFileVisibility>,
        statuses: List<StorageFileStatus>,
        mimeTypes: List<String>,
        fileNames: List<String>
    ): AppResult<PagedResult<StorageFileSummary>> = dbQuery {
        if (!managementUserPermissionCodes.contains(StoragePermissionCode.STORAGE_VIEW_OTHER_USERS_FILES)) {
            return@dbQuery AppResult.Error(StorageError.FileNotFound(null))
        }

        val getFilesResult = filesRepository.getStorageFilesPageForManagement(
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

        val expirationSeconds = settingsProvider.getPresignedUrlExpirationSeconds()

        getFilesResult.mapSuccess { paged ->
            paged.mapItems { fileInternal ->
                val fileUrl = storageService.generatePresignedGetUrl(
                    key = fileInternal.s3Key,
                    expirationSeconds = expirationSeconds
                ).dataOrNull().orEmpty()

                fileInternal.toStorageFileSummary(fileUrl = fileUrl)
            }
        }
    }

    override suspend fun cleanupDeletedStorageFiles(batchSize: Int?): AppResult<Int> = dbQuery {
        val effectiveBatchSize = batchSize ?: storageConfig.maintenanceBatchSize
        val filesResult = filesRepository.getStorageFilesByStatus(
            status = StorageFileStatus.DELETED,
            limit = effectiveBatchSize
        )

        val deletedFiles = when (filesResult) {
            is AppResult.Success -> filesResult.data
            is AppResult.Error -> return@dbQuery filesResult
        }

        if (deletedFiles.isEmpty()) {
            return@dbQuery AppResult.Success(0)
        }

        val s3Keys = deletedFiles.map { it.s3Key }
        val deleteStorageResult = storageService.deleteBatch(s3Keys)
        if (deleteStorageResult is AppResult.Error) {
            return@dbQuery deleteStorageResult
        }

        val fileIds = deletedFiles.map { it.id }
        val hardDeleteResult = filesRepository.hardDeleteStorageFilesBatch(fileIds)
        if (hardDeleteResult is AppResult.Error) {
            return@dbQuery hardDeleteResult
        }

        AppResult.Success(deletedFiles.size)
    }

    override suspend fun processExpiredPendingUploads(batchSize: Int?): AppResult<Int> = dbQuery {
        val effectiveBatchSize = batchSize ?: storageConfig.maintenanceBatchSize
        val expirationSeconds = settingsProvider.getPresignedUrlExpirationSeconds()
        val createdBefore = Clock.System.now() - expirationSeconds.seconds

        val filesResult = filesRepository.getExpiredPendingUploadFiles(
            createdBefore = createdBefore,
            limit = effectiveBatchSize
        )

        val pendingFiles = when (filesResult) {
            is AppResult.Success -> filesResult.data
            is AppResult.Error -> return@dbQuery filesResult
        }

        if (pendingFiles.isEmpty()) {
            return@dbQuery AppResult.Success(0)
        }

        val readyFiles = mutableListOf<StorageFileInternal>()
        val abandonedFileIds = mutableListOf<FileId>()

        coroutineScope {
            val results = pendingFiles.map { file ->
                async {
                    val metadataResult = storageService.getObjectMetadata(file.s3Key)
                    file to metadataResult
                }
            }.awaitAll()

            results.forEach { (file, metadataResult) ->
                if (metadataResult is AppResult.Success) {
                    val metadata = metadataResult.data
                    readyFiles.add(
                        file.copy(
                            status = StorageFileStatus.READY,
                            fileSizeBytes = metadata.sizeBytes,
                            contentHash = metadata.eTag
                        )
                    )
                } else {
                    abandonedFileIds.add(file.id)
                }
            }
        }

        if (readyFiles.isNotEmpty()) {
            val updateResult = filesRepository.updateStorageFilesBatch(readyFiles)
            if (updateResult is AppResult.Error) return@dbQuery updateResult
        }

        if (abandonedFileIds.isNotEmpty()) {
            val deleteResult = filesRepository.hardDeleteStorageFilesBatch(abandonedFileIds)
            if (deleteResult is AppResult.Error) return@dbQuery deleteResult
        }

        AppResult.Success(readyFiles.size + abandonedFileIds.size)
    }
}