package io.github.mudrichenkoevgeny.backend.core.storage.database.repository

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.model.onSet
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.pagination.getNumOfTotalPages
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.toJavaInstant
import io.github.mudrichenkoevgeny.backend.core.common.util.toKotlinInstant
import io.github.mudrichenkoevgeny.backend.core.database.extensions.applyPagination
import io.github.mudrichenkoevgeny.backend.core.database.extensions.substringSqlLikePattern
import io.github.mudrichenkoevgeny.backend.core.database.mapper.toExposedSortOrder
import io.github.mudrichenkoevgeny.backend.core.storage.database.table.StorageFilesTable
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFileInternal
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.listing.StorageSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.status.StorageFileStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

/**
 * Exposed-backed implementation of [StorageFilesRepository].
 *
 * Executes SQL queries against PostgreSQL using Exposed DSL mapped to [StorageFilesTable].
 *
 */
@Singleton
class StorageFilesRepositoryImpl @Inject constructor() : StorageFilesRepository {

    override suspend fun createStorageFile(storageFile: StorageFileInternal): AppResult<StorageFileInternal> {
        return try {
            val now = Clock.System.now()
            StorageFilesTable.upsert(StorageFilesTable.id) { row ->
                row[id] = storageFile.id.value
                row[ownerUserId] = storageFile.ownerUserId
                row[fileName] = storageFile.fileName
                row[fileSizeBytes] = storageFile.fileSizeBytes
                row[mimeType] = storageFile.mimeType
                row[category] = storageFile.category
                row[visibility] = storageFile.visibility
                row[status] = storageFile.status
                row[targetScope] = storageFile.targetScope
                row[contentHash] = storageFile.contentHash
                row[s3Bucket] = storageFile.s3Bucket
                row[s3Key] = storageFile.s3Key
                row[createdAt] = storageFile.createdAt.toJavaInstant()
                row[updatedAt] = now.toJavaInstant()
            }
            AppResult.Success(storageFile.copy(updatedAt = now))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(CommonError.Database("Error saving storage file: ${e.message}"))
        }
    }

    override suspend fun deleteStorageFile(fileId: FileId): AppResult<Unit> {
        return try {
            StorageFilesTable.update({ StorageFilesTable.id eq fileId.value }) { row ->
                row[status] = StorageFileStatus.DELETED
                row[updatedAt] = Clock.System.now().toJavaInstant()
            }
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(CommonError.Database("Error deleting storage file $fileId: ${e.message}"))
        }
    }

    override suspend fun deleteStorageFile(fileId: FileId, ownerUserId: String?): AppResult<Unit> {
        return try {
            StorageFilesTable.update({
                val condition = StorageFilesTable.id eq fileId.value
                if (ownerUserId != null) {
                    condition and (StorageFilesTable.ownerUserId eq ownerUserId)
                } else {
                    condition
                }
            }) { row ->
                row[status] = StorageFileStatus.DELETED
                row[updatedAt] = Clock.System.now().toJavaInstant()
            }
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(CommonError.Database("Error deleting storage file $fileId: ${e.message}"))
        }
    }

    override suspend fun deleteStorageFilesBatch(fileIds: List<FileId>): AppResult<Unit> {
        if (fileIds.isEmpty()) return AppResult.Success(Unit)

        return try {
            val rawIds = fileIds.map { it.value }
            StorageFilesTable.update({ StorageFilesTable.id inList rawIds }) { row ->
                row[status] = StorageFileStatus.DELETED
                row[updatedAt] = Clock.System.now().toJavaInstant()
            }
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(CommonError.Database("Error deleting batch storage files: ${e.message}"))
        }
    }

    override suspend fun deleteStorageFilesBatch(fileIds: List<FileId>, ownerUserId: String?): AppResult<Unit> {
        if (fileIds.isEmpty()) return AppResult.Success(Unit)

        return try {
            val rawIds = fileIds.map { it.value }
            StorageFilesTable.update({
                val condition = StorageFilesTable.id inList rawIds
                if (ownerUserId != null) {
                    condition and (StorageFilesTable.ownerUserId eq ownerUserId)
                } else {
                    condition
                }
            }) { row ->
                row[status] = StorageFileStatus.DELETED
                row[updatedAt] = Clock.System.now().toJavaInstant()
            }
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(CommonError.Database("Error deleting batch storage files: ${e.message}"))
        }
    }

    override suspend fun updateStorageFile(
        fileId: FileId,
        status: UpdateField<StorageFileStatus>,
        fileSizeBytes: UpdateField<Long>,
        contentHash: UpdateField<String?>
    ): AppResult<StorageFileInternal> {
        return try {
            val updatedRows = StorageFilesTable.update({ StorageFilesTable.id eq fileId.value }) { updateStatement ->
                status.onSet { if (it != null) updateStatement[StorageFilesTable.status] = it }
                fileSizeBytes.onSet { if (it != null) updateStatement[StorageFilesTable.fileSizeBytes] = it }
                contentHash.onSet { updateStatement[StorageFilesTable.contentHash] = it }

                updateStatement[StorageFilesTable.updatedAt] = Clock.System.now().toJavaInstant()
            }

            if (updatedRows == 0) {
                AppResult.Error(CommonError.Database("Failed to update storage file id=${fileId.value}"))
            } else {
                getStorageFileById(fileId).let {
                    if (it is AppResult.Success && it.data != null) {
                        AppResult.Success(it.data!!)
                    } else {
                        AppResult.Error(CommonError.Database("Storage file updated but could not be retrieved"))
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(CommonError.Database("Error updating storage file: ${e.message}"))
        }
    }

    override suspend fun countPendingUploadsForUser(ownerUserId: String): AppResult<Long> {
        return try {
            val count = StorageFilesTable.selectAll()
                .where { StorageFilesTable.ownerUserId eq ownerUserId }
                .andWhere { StorageFilesTable.status eq StorageFileStatus.PENDING_UPLOAD }
                .count()
            AppResult.Success(count)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(CommonError.Database("Error counting pending uploads: ${e.message}"))
        }
    }

    override suspend fun calculateUsedStorageForUser(ownerUserId: String): AppResult<Long> {
        return try {
            val rows = StorageFilesTable.selectAll()
                .where { StorageFilesTable.ownerUserId eq ownerUserId }
                .andWhere { StorageFilesTable.status inList listOf(StorageFileStatus.READY, StorageFileStatus.PROCESSING) }
            val usedBytes: Long = rows.sumOf { it[StorageFilesTable.fileSizeBytes] }
            AppResult.Success(usedBytes)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(CommonError.Database("Error calculating used storage: ${e.message}"))
        }
    }

    override suspend fun getStorageFileById(fileId: FileId): AppResult<StorageFileInternal?> {
        return try {
            val row = StorageFilesTable.selectAll().where { StorageFilesTable.id eq fileId.value }.singleOrNull()
            AppResult.Success(row?.toStorageFileInternal())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(CommonError.Database("Error retrieving storage file $fileId: ${e.message}"))
        }
    }
    override suspend fun getStorageFilesPageForManagement(
        pageParams: PageParams,
        sortBy: StorageSortValues.StorageFileSortBy,
        sortOrder: SortOrder,
        ownerUserIds: List<String>,
        categories: List<StorageCategory>,
        visibilities: List<StorageFileVisibility>,
        statuses: List<StorageFileStatus>,
        mimeTypes: List<String>,
        fileNames: List<String>
    ): AppResult<PagedResult<StorageFileInternal>> {
        val query = StorageFilesTable.selectAll()

        if (ownerUserIds.isNotEmpty()) {
            query.andWhere { StorageFilesTable.ownerUserId inList ownerUserIds }
        }

        return executeStorageFilesPagedQuery(
            query = query,
            pageParams = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            categories = categories,
            visibilities = visibilities,
            statuses = statuses,
            mimeTypes = mimeTypes,
            fileNames = fileNames
        )
    }

    override suspend fun getStorageFilesPageByOwnerUserId(
        ownerUserId: String,
        pageParams: PageParams,
        sortBy: StorageSortValues.StorageFileSortBy,
        sortOrder: SortOrder,
        categories: List<StorageCategory>,
        visibilities: List<StorageFileVisibility>,
        statuses: List<StorageFileStatus>,
        mimeTypes: List<String>,
        fileNames: List<String>
    ): AppResult<PagedResult<StorageFileInternal>> {
        val query = StorageFilesTable
            .selectAll()
            .where { StorageFilesTable.ownerUserId eq ownerUserId }

        if (statuses.isEmpty()) {
            query.andWhere { StorageFilesTable.status neq StorageFileStatus.DELETED }
        }

        return executeStorageFilesPagedQuery(
            query = query,
            pageParams = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            categories = categories,
            visibilities = visibilities,
            statuses = statuses,
            mimeTypes = mimeTypes,
            fileNames = fileNames
        )
    }

    private fun executeStorageFilesPagedQuery(
        query: Query,
        pageParams: PageParams,
        sortBy: StorageSortValues.StorageFileSortBy,
        sortOrder: SortOrder,
        categories: List<StorageCategory>,
        visibilities: List<StorageFileVisibility>,
        statuses: List<StorageFileStatus>,
        mimeTypes: List<String>,
        fileNames: List<String>
    ): AppResult<PagedResult<StorageFileInternal>> {
        return try {
            if (categories.isNotEmpty()) {
                query.andWhere { StorageFilesTable.category inList categories }
            }
            if (visibilities.isNotEmpty()) {
                query.andWhere { StorageFilesTable.visibility inList visibilities }
            }
            if (statuses.isNotEmpty()) {
                query.andWhere { StorageFilesTable.status inList statuses }
            }

            addOrLikeFilter(query, StorageFilesTable.mimeType, mimeTypes)
            addOrLikeFilter(query, StorageFilesTable.fileName, fileNames)

            val totalCount = query.count()
            val totalPages = getNumOfTotalPages(totalCount, pageParams.size)

            val sortColumn = when (sortBy) {
                StorageSortValues.StorageFileSortBy.CREATED_AT -> StorageFilesTable.createdAt
                StorageSortValues.StorageFileSortBy.UPDATED_AT -> StorageFilesTable.updatedAt
                StorageSortValues.StorageFileSortBy.FILE_NAME -> StorageFilesTable.fileName
                StorageSortValues.StorageFileSortBy.FILE_SIZE_BYTES -> StorageFilesTable.fileSizeBytes
                StorageSortValues.StorageFileSortBy.DEFAULT -> StorageFilesTable.createdAt
            }

            val items = query
                .orderBy(sortColumn to sortOrder.toExposedSortOrder())
                .applyPagination(pageParams)
                .map { it.toStorageFileInternal() }

            AppResult.Success(
                PagedResult(
                    items = items,
                    totalCount = totalCount,
                    pageNumber = pageParams.page,
                    pageSize = pageParams.size,
                    totalPages = totalPages
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(CommonError.Database("Error executing storage files paged query: ${e.message}"))
        }
    }

    // todo move to another place and reuse
    private fun <T : String?> addOrLikeFilter(
        query: Query,
        column: Column<T>,
        rawValues: List<String>
    ) {
        val nonBlank = rawValues.filter { it.isNotBlank() }
        if (nonBlank.isEmpty()) {
            return
        }

        val predicate = nonBlank
            .map { value ->
                (column.lowerCase() like substringSqlLikePattern(value.lowercase())) as Op<Boolean>
            }
            .reduce { acc, condition -> acc or condition }

        query.andWhere { predicate }
    }

    private fun ResultRow.toStorageFileInternal(): StorageFileInternal {
        return StorageFileInternal(
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
            createdAt = this[StorageFilesTable.createdAt].toKotlinInstant(),
            updatedAt = this[StorageFilesTable.updatedAt]?.toKotlinInstant()
        )
    }

    override suspend fun getStorageFilesByStatus(
        status: StorageFileStatus,
        limit: Int
    ): AppResult<List<StorageFileInternal>> {
        return try {
            val items = StorageFilesTable.selectAll()
                .where { StorageFilesTable.status eq status }
                .limit(limit)
                .map { it.toStorageFileInternal() }
            AppResult.Success(items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(CommonError.Database("Error retrieving files by status: ${e.message}"))
        }
    }

    override suspend fun hardDeleteStorageFilesBatch(fileIds: List<FileId>): AppResult<Unit> {
        if (fileIds.isEmpty()) {
            return AppResult.Success(Unit)
        }

        return try {
            val rawIds = fileIds.map { it.value }
            StorageFilesTable.deleteWhere { StorageFilesTable.id inList rawIds }
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(CommonError.Database("Error hard deleting storage files batch: ${e.message}"))
        }
    }
}