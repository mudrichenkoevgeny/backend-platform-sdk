package io.github.mudrichenkoevgeny.backend.core.storage.database.repository

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.model.onSet
import io.github.mudrichenkoevgeny.backend.core.common.pagination.ListingQueryParams
import io.github.mudrichenkoevgeny.backend.core.common.pagination.getNumOfTotalPages
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.toJavaInstant
import io.github.mudrichenkoevgeny.backend.core.common.util.toKotlinInstant
import io.github.mudrichenkoevgeny.backend.core.database.dsl.addOrLikeFilter
import io.github.mudrichenkoevgeny.backend.core.database.dsl.applyNullabilityFilter
import io.github.mudrichenkoevgeny.backend.core.database.dsl.applyPagination
import io.github.mudrichenkoevgeny.backend.core.database.dsl.applyTimestampFilter
import io.github.mudrichenkoevgeny.backend.core.database.dsl.substringSqlLikePattern
import io.github.mudrichenkoevgeny.backend.core.database.mapper.toExposedSortOrder
import io.github.mudrichenkoevgeny.backend.core.storage.database.table.StorageFilesTable
import io.github.mudrichenkoevgeny.backend.core.storage.network.model.ManagementStorageFileListQueryParams
import io.github.mudrichenkoevgeny.backend.core.storage.network.model.SelfStorageFileListQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFileInternal
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.listing.StorageFileSortBy
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.status.StorageFileStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant
import java.time.Instant as JavaInstant

/**
 * Default [StorageFilesRepository] implementation backed by Exposed and [StorageFilesTable].
 *
 * Performs synchronous Exposed DSL operations and maps [ResultRow] values into [StorageFileInternal].
 * Returns [CommonError.Database] when inserts/updates report no affected rows.
 */
@Singleton
public class StorageFilesRepositoryImpl @Inject constructor() : StorageFilesRepository {
    override suspend fun createStorageFile(storageFile: StorageFileInternal): AppResult<StorageFileInternal> {
        return try {
            val inserted = StorageFilesTable.insert { row ->
                row.applyStorageFile(storageFile)
            }

            if (inserted.insertedCount == 0) {
                return AppResult.Error(
                    CommonError.Database("Failed to insert storage file: ${storageFile.id.value}")
                )
            }

            AppResult.Success(storageFile)
        } catch (e: ExposedSQLException) {
            AppResult.Error(CommonError.Database("Error creating storage file: ${e.message}"))
        }
    }

    override suspend fun deleteStorageFile(fileId: FileId): AppResult<Unit> = deleteStorageFile(
        fileId,
        null
    )

    override suspend fun deleteStorageFile(
        fileId: FileId,
        ownerUserId: String?
    ): AppResult<Unit> = try {
        StorageFilesTable.deleteWhere {
            val base = StorageFilesTable.id eq fileId.value
            if (ownerUserId != null) {
                base and (StorageFilesTable.ownerUserId eq ownerUserId)
            } else {
                base
            }
        }
        AppResult.Success(Unit)
    } catch (e: ExposedSQLException) {
        AppResult.Error(CommonError.Database("Error deleting storage file $fileId: ${e.message}"))
    }

    override suspend fun deleteStorageFilesBatch(fileIds: List<FileId>): AppResult<Unit> = deleteStorageFilesBatch(
        fileIds,
        null
    )

    override suspend fun deleteStorageFilesBatch(
        fileIds: List<FileId>,
        ownerUserId: String?
    ): AppResult<Unit> {
        if (fileIds.isEmpty()) return AppResult.Success(Unit)
        return try {
            val ids = fileIds.map { it.value }
            StorageFilesTable.deleteWhere {
                val base = StorageFilesTable.id inList ids
                if (ownerUserId != null) {
                    base and (StorageFilesTable.ownerUserId eq ownerUserId)
                } else {
                    base
                }
            }
            AppResult.Success(Unit)
        } catch (e: ExposedSQLException) {
            AppResult.Error(CommonError.Database("Error deleting storage files batch: ${e.message}"))
        }
    }

    override suspend fun updateStorageFile(
        fileId: FileId,
        status: UpdateField<StorageFileStatus>,
        fileSizeBytes: UpdateField<Long>,
        contentHash: UpdateField<String?>
    ): AppResult<StorageFileInternal> {
        return try {
            val updatedRows = StorageFilesTable.update({ StorageFilesTable.id eq fileId.value }) { row ->
                status.onSet { newStatus ->
                    newStatus?.let { row[StorageFilesTable.status] = it }
                }
                fileSizeBytes.onSet { newFileSizeBytes ->
                    newFileSizeBytes?.let { row[StorageFilesTable.fileSizeBytes] = it }
                }
                contentHash.onSet { newContentHash ->
                    row[StorageFilesTable.contentHash] = newContentHash
                }
                row[StorageFilesTable.updatedAt] = JavaInstant.now()
            }

            if (updatedRows == 0) {
                return AppResult.Error(CommonError.Database("Failed to update storage file $fileId"))
            }

            val fileResult = getStorageFileById(fileId)
            when (fileResult) {
                is AppResult.Success -> {
                    val file = fileResult.data
                    if (file != null) {
                        AppResult.Success(file)
                    } else {
                        AppResult.Error(CommonError.Database("Storage file $fileId not found after update"))
                    }
                }

                is AppResult.Error -> AppResult.Error(fileResult.error)
            }
        } catch (e: ExposedSQLException) {
            AppResult.Error(CommonError.Database("Error updating storage file $fileId: ${e.message}"))
        }
    }

    override suspend fun countPendingUploadsForUser(ownerUserId: String): AppResult<Long> = try {
        val countExpression = StorageFilesTable.id.count()
        val count = StorageFilesTable
            .select(countExpression)
            .where {
                (StorageFilesTable.ownerUserId eq ownerUserId) and
                    (StorageFilesTable.status eq StorageFileStatus.PENDING_UPLOAD)
            }.firstOrNull()
            ?.get(countExpression) ?: 0L
        AppResult.Success(count)
    } catch (e: ExposedSQLException) {
        AppResult.Error(
            CommonError.Database("Error counting pending uploads for user $ownerUserId: ${e.message}")
        )
    }

    override suspend fun calculateUsedStorageForUser(ownerUserId: String): AppResult<Long> = try {
        val sumExpression = StorageFilesTable.fileSizeBytes.sum()
        val sum = StorageFilesTable
            .select(sumExpression)
            .where {
                (StorageFilesTable.ownerUserId eq ownerUserId) and (StorageFilesTable.status eq StorageFileStatus.READY)
            }.firstOrNull()
            ?.get(sumExpression) ?: 0L
        AppResult.Success(sum)
    } catch (e: ExposedSQLException) {
        AppResult.Error(
            CommonError.Database("Error calculating used storage for user $ownerUserId: ${e.message}")
        )
    }

    override suspend fun getStorageFileById(fileId: FileId): AppResult<StorageFileInternal?> = try {
        val row = StorageFilesTable.selectAll().where { StorageFilesTable.id eq fileId.value }.singleOrNull()
        AppResult.Success(row?.toStorageFileInternal())
    } catch (e: ExposedSQLException) {
        AppResult.Error(CommonError.Database("Error retrieving storage file $fileId: ${e.message}"))
    }

    override suspend fun getStorageFilesPageForManagement(
        queryParams: ManagementStorageFileListQueryParams
    ): AppResult<PagedResult<StorageFileInternal>> {
        val query = StorageFilesTable.selectAll()

        if (queryParams.ownerUserIds.isNotEmpty()) {
            query.andWhere { StorageFilesTable.ownerUserId inList queryParams.ownerUserIds }
        }

        return executeStorageFilesPagedQuery(
            query = query,
            queryParams = queryParams
        )
    }

    override suspend fun getStorageFilesPageByOwnerUserId(
        ownerUserId: String,
        queryParams: SelfStorageFileListQueryParams
    ): AppResult<PagedResult<StorageFileInternal>> {
        val query = StorageFilesTable.selectAll().where { StorageFilesTable.ownerUserId eq ownerUserId }

        if (queryParams.statuses.isEmpty()) {
            query.andWhere { StorageFilesTable.status neq StorageFileStatus.DELETED }
        }

        return executeStorageFilesPagedQuery(
            query = query,
            queryParams = queryParams
        )
    }

    private fun executeStorageFilesPagedQuery(
        query: Query,
        queryParams: ManagementStorageFileListQueryParams
    ): AppResult<PagedResult<StorageFileInternal>> = executeStorageFilesPagedQueryCommon(
        query = query,
        listing = queryParams.listing,
        fileIds = queryParams.fileIds,
        isOwnerUserIdSet = queryParams.isOwnerUserIdSet,
        fileName = queryParams.fileName,
        fileSizeBytesFrom = queryParams.fileSizeBytesFrom,
        fileSizeBytesTo = queryParams.fileSizeBytesTo,
        mimeTypes = queryParams.mimeTypes,
        categories = queryParams.categories,
        excludeCategories = queryParams.excludeCategories,
        visibilities = queryParams.visibilities,
        excludeVisibilities = queryParams.excludeVisibilities,
        statuses = queryParams.statuses,
        excludeStatuses = queryParams.excludeStatuses,
        targetScopes = queryParams.targetScopes,
        isTargetScopeSet = queryParams.isTargetScopeSet,
        createdAtFrom = queryParams.createdAtFrom,
        createdAtTo = queryParams.createdAtTo,
        updatedAtFrom = queryParams.updatedAtFrom,
        updatedAtTo = queryParams.updatedAtTo,
        isUpdatedAtSet = queryParams.isUpdatedAtSet,
        searchQuery = queryParams.searchQuery
    )

    private fun executeStorageFilesPagedQuery(
        query: Query,
        queryParams: SelfStorageFileListQueryParams
    ): AppResult<PagedResult<StorageFileInternal>> = executeStorageFilesPagedQueryCommon(
        query = query,
        listing = queryParams.listing,
        fileIds = queryParams.fileIds,
        isOwnerUserIdSet = null,
        fileName = queryParams.fileName,
        fileSizeBytesFrom = queryParams.fileSizeBytesFrom,
        fileSizeBytesTo = queryParams.fileSizeBytesTo,
        mimeTypes = queryParams.mimeTypes,
        categories = queryParams.categories,
        excludeCategories = queryParams.excludeCategories,
        visibilities = queryParams.visibilities,
        excludeVisibilities = queryParams.excludeVisibilities,
        statuses = queryParams.statuses,
        excludeStatuses = queryParams.excludeStatuses,
        targetScopes = queryParams.targetScopes,
        isTargetScopeSet = queryParams.isTargetScopeSet,
        createdAtFrom = queryParams.createdAtFrom,
        createdAtTo = queryParams.createdAtTo,
        updatedAtFrom = queryParams.updatedAtFrom,
        updatedAtTo = queryParams.updatedAtTo,
        isUpdatedAtSet = queryParams.isUpdatedAtSet,
        searchQuery = queryParams.searchQuery
    )

    private fun executeStorageFilesPagedQueryCommon(
        query: Query,
        listing: ListingQueryParams<StorageFileSortBy>,
        fileIds: List<FileId>,
        isOwnerUserIdSet: Boolean?,
        fileName: String?,
        fileSizeBytesFrom: Long?,
        fileSizeBytesTo: Long?,
        mimeTypes: List<String>,
        categories: List<StorageCategory>,
        excludeCategories: List<StorageCategory>,
        visibilities: List<StorageFileVisibility>,
        excludeVisibilities: List<StorageFileVisibility>,
        statuses: List<StorageFileStatus>,
        excludeStatuses: List<StorageFileStatus>,
        targetScopes: List<String>,
        isTargetScopeSet: Boolean?,
        createdAtFrom: Instant?,
        createdAtTo: Instant?,
        updatedAtFrom: Instant?,
        updatedAtTo: Instant?,
        isUpdatedAtSet: Boolean?,
        searchQuery: String?
    ): AppResult<PagedResult<StorageFileInternal>> = try {
        if (fileIds.isNotEmpty()) {
            val uuids = fileIds.map { it.value }
            query.andWhere { StorageFilesTable.id inList uuids }
        }
        if (isOwnerUserIdSet == true) {
            query.andWhere { StorageFilesTable.ownerUserId.isNotNull() }
        } else if (isOwnerUserIdSet == false) {
            query.andWhere { StorageFilesTable.ownerUserId.isNull() }
        }
        if (!fileName.isNullOrBlank()) {
            query.addOrLikeFilter(
                StorageFilesTable.fileName,
                listOf(fileName)
            )
        }
        if (fileSizeBytesFrom != null) {
            query.andWhere { StorageFilesTable.fileSizeBytes greaterEq fileSizeBytesFrom }
        }
        if (fileSizeBytesTo != null) {
            query.andWhere { StorageFilesTable.fileSizeBytes lessEq fileSizeBytesTo }
        }
        if (categories.isNotEmpty()) {
            query.andWhere { StorageFilesTable.category inList categories }
        }
        if (excludeCategories.isNotEmpty()) {
            query.andWhere { StorageFilesTable.category notInList excludeCategories }
        }
        if (visibilities.isNotEmpty()) {
            query.andWhere { StorageFilesTable.visibility inList visibilities }
        }
        if (excludeVisibilities.isNotEmpty()) {
            query.andWhere { StorageFilesTable.visibility notInList excludeVisibilities }
        }
        if (statuses.isNotEmpty()) {
            query.andWhere { StorageFilesTable.status inList statuses }
        }
        if (excludeStatuses.isNotEmpty()) {
            query.andWhere { StorageFilesTable.status notInList excludeStatuses }
        }
        if (targetScopes.isNotEmpty()) {
            query.andWhere { StorageFilesTable.targetScope inList targetScopes }
        }
        query.applyNullabilityFilter(
            StorageFilesTable.targetScope,
            isTargetScopeSet
        )

        query.addOrLikeFilter(
            StorageFilesTable.mimeType,
            mimeTypes
        )

        query.applyTimestampFilter(
            StorageFilesTable.createdAt,
            createdAtFrom,
            createdAtTo
        )
        query.applyTimestampFilter(
            StorageFilesTable.updatedAt,
            updatedAtFrom,
            updatedAtTo,
            isUpdatedAtSet
        )

        if (!searchQuery.isNullOrBlank()) {
            val pattern = substringSqlLikePattern(searchQuery.lowercase())
            query.andWhere { StorageFilesTable.fileName.lowerCase() like pattern }
        }

        val totalCount = query.count()
        val totalPages = getNumOfTotalPages(
            totalCount,
            listing.pageParams.size
        )

        val sortColumn = when (listing.sortBy) {
            StorageFileSortBy.CREATED_AT -> StorageFilesTable.createdAt
            StorageFileSortBy.UPDATED_AT -> StorageFilesTable.updatedAt
            StorageFileSortBy.FILE_NAME -> StorageFilesTable.fileName
            StorageFileSortBy.FILE_SIZE_BYTES -> StorageFilesTable.fileSizeBytes
        }

        val items = query
            .orderBy(sortColumn to listing.sortOrder.toExposedSortOrder())
            .applyPagination(listing.pageParams)
            .map { it.toStorageFileInternal() }

        AppResult.Success(
            PagedResult(
                items = items,
                totalCount = totalCount,
                pageNumber = listing.pageParams.page,
                pageSize = listing.pageParams.size,
                totalPages = totalPages
            )
        )
    } catch (e: ExposedSQLException) {
        AppResult.Error(CommonError.Database("Error executing storage files paged query: ${e.message}"))
    }

    override suspend fun getStorageFilesByStatus(
        status: StorageFileStatus,
        limit: Int
    ): AppResult<List<StorageFileInternal>> = try {
        val items = StorageFilesTable
            .selectAll()
            .where { StorageFilesTable.status eq status }
            .limit(limit)
            .map { it.toStorageFileInternal() }
        AppResult.Success(items)
    } catch (e: ExposedSQLException) {
        AppResult.Error(CommonError.Database("Error retrieving files by status: ${e.message}"))
    }

    override suspend fun hardDeleteStorageFilesBatch(fileIds: List<FileId>): AppResult<Unit> {
        if (fileIds.isEmpty()) return AppResult.Success(Unit)
        return try {
            val ids = fileIds.map { it.value }
            StorageFilesTable.deleteWhere { StorageFilesTable.id inList ids }
            AppResult.Success(Unit)
        } catch (e: ExposedSQLException) {
            AppResult.Error(CommonError.Database("Error hard deleting storage files batch: ${e.message}"))
        }
    }

    override suspend fun getExpiredPendingUploadFiles(
        createdBefore: Instant,
        limit: Int
    ): AppResult<List<StorageFileInternal>> = try {
        val items = StorageFilesTable
            .selectAll()
            .where {
                (StorageFilesTable.status eq StorageFileStatus.PENDING_UPLOAD) and
                    (StorageFilesTable.createdAt lessEq createdBefore.toJavaInstant())
            }.limit(limit)
            .map { it.toStorageFileInternal() }
        AppResult.Success(items)
    } catch (e: ExposedSQLException) {
        AppResult.Error(CommonError.Database("Error retrieving expired pending upload files: ${e.message}"))
    }

    override suspend fun updateStorageFilesBatch(files: List<StorageFileInternal>): AppResult<Unit> {
        if (files.isEmpty()) return AppResult.Success(Unit)
        return try {
            StorageFilesTable.batchUpsert(
                data = files,
                keys = arrayOf(StorageFilesTable.id)
            ) { file ->
                applyStorageFile(
                    file = file,
                    updatedAt = JavaInstant.now()
                )
            }
            AppResult.Success(Unit)
        } catch (e: ExposedSQLException) {
            AppResult.Error(CommonError.Database("Error updating storage files batch: ${e.message}"))
        }
    }

    private fun ResultRow.toStorageFileInternal(): StorageFileInternal = StorageFileInternal(
        id = FileId(this[StorageFilesTable.id].value),
        ownerUserId = this[StorageFilesTable.ownerUserId],
        fileName = this[StorageFilesTable.fileName],
        fileSizeBytes = this[StorageFilesTable.fileSizeBytes],
        mimeType = this[StorageFilesTable.mimeType],
        category = this[StorageFilesTable.category],
        visibility = this[StorageFilesTable.visibility],
        status = this[StorageFilesTable.status],
        targetScope = this[StorageFilesTable.targetScope],
        contentHash = this[StorageFilesTable.contentHash],
        s3Bucket = this[StorageFilesTable.s3Bucket],
        s3Key = this[StorageFilesTable.s3Key],
        fileUrls = null,
        createdAt = this[StorageFilesTable.createdAt].toKotlinInstant(),
        updatedAt = this[StorageFilesTable.updatedAt]?.toKotlinInstant()
    )

    private fun UpdateBuilder<*>.applyStorageFile(
        file: StorageFileInternal,
        updatedAt: JavaInstant? = file.updatedAt?.toJavaInstant()
    ) {
        this[StorageFilesTable.id] = file.id.value
        this[StorageFilesTable.ownerUserId] = file.ownerUserId
        this[StorageFilesTable.fileName] = file.fileName
        this[StorageFilesTable.fileSizeBytes] = file.fileSizeBytes
        this[StorageFilesTable.mimeType] = file.mimeType
        this[StorageFilesTable.category] = file.category
        this[StorageFilesTable.visibility] = file.visibility
        this[StorageFilesTable.status] = file.status
        this[StorageFilesTable.targetScope] = file.targetScope
        this[StorageFilesTable.contentHash] = file.contentHash
        this[StorageFilesTable.s3Bucket] = file.s3Bucket
        this[StorageFilesTable.s3Key] = file.s3Key
        this[StorageFilesTable.createdAt] = file.createdAt.toJavaInstant()
        this[StorageFilesTable.updatedAt] = updatedAt
    }
}
