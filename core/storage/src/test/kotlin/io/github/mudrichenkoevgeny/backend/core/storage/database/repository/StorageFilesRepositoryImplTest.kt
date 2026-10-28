package io.github.mudrichenkoevgeny.backend.core.storage.database.repository

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.createTestDataSource
import io.github.mudrichenkoevgeny.backend.core.storage.database.table.StorageFilesTable
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFileInternal
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.status.StorageFileStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StorageFilesRepositoryImplTest {

    private val dataSource = createTestDataSource("storage_files_repo")
    private lateinit var repository: StorageFilesRepository

    @BeforeAll
    fun setupAll() {
        Database.connect(dataSource)
        runBlocking {
            suspendTransaction {
                SchemaUtils.create(StorageFilesTable)
            }
        }
        repository = StorageFilesRepositoryImpl()
    }

    @BeforeEach
    fun setup() {
        runBlocking {
            suspendTransaction {
                StorageFilesTable.deleteAll()
            }
        }
    }

    private fun createSampleFile(
        id: FileId = FileId.generate(),
        ownerUserId: String = "usr_123",
        status: StorageFileStatus = StorageFileStatus.PENDING_UPLOAD,
        createdAt: kotlin.time.Instant = Clock.System.now()
    ) = StorageFileInternal(
        id = id,
        ownerUserId = ownerUserId,
        fileName = "test.png",
        fileSizeBytes = 100L,
        mimeType = "image/png",
        category = StorageCategory.IMAGE,
        visibility = StorageFileVisibility.PUBLIC,
        status = status,
        targetScope = "avatar",
        contentHash = "hash",
        s3Bucket = "files",
        s3Key = "uploads/test.png",
        createdAt = createdAt,
        updatedAt = createdAt
    )

    @Test
    fun `getExpiredPendingUploadFiles returns pending files created before threshold`() = runBlocking {
        val now = Clock.System.now()
        val oldFile = createSampleFile(
            id = FileId.generate(),
            status = StorageFileStatus.PENDING_UPLOAD,
            createdAt = now - 2.hours
        )
        val newFile = createSampleFile(
            id = FileId.generate(),
            status = StorageFileStatus.PENDING_UPLOAD,
            createdAt = now
        )
        val readyFile = createSampleFile(
            id = FileId.generate(),
            status = StorageFileStatus.READY,
            createdAt = now - 2.hours
        )

        suspendTransaction {
            repository.createStorageFile(oldFile)
            repository.createStorageFile(newFile)
            repository.createStorageFile(readyFile)
        }

        val result = suspendTransaction {
            repository.getExpiredPendingUploadFiles(createdBefore = now - 1.hours, limit = 10)
        }

        assertTrue(result is AppResult.Success)
        val files = (result as AppResult.Success).data
        assertEquals(1, files.size)
        assertEquals(oldFile.id, files.first().id)
    }

    @Test
    fun `updateStorageFilesBatch updates multiple file records`() = runBlocking {
        val file1 = createSampleFile(status = StorageFileStatus.PENDING_UPLOAD)
        val file2 = createSampleFile(status = StorageFileStatus.PENDING_UPLOAD)

        suspendTransaction {
            repository.createStorageFile(file1)
            repository.createStorageFile(file2)
        }

        val updated1 = file1.copy(status = StorageFileStatus.READY, fileSizeBytes = 500L, contentHash = "new-hash-1")
        val updated2 = file2.copy(status = StorageFileStatus.READY, fileSizeBytes = 700L, contentHash = "new-hash-2")

        suspendTransaction {
            repository.updateStorageFilesBatch(listOf(updated1, updated2))
        }

        val file1Db = suspendTransaction { repository.getStorageFileById(file1.id) }
        val file2Db = suspendTransaction { repository.getStorageFileById(file2.id) }

        assertEquals(StorageFileStatus.READY, (file1Db as AppResult.Success).data?.status)
        assertEquals(500L, file1Db.data?.fileSizeBytes)
        assertEquals("new-hash-1", file1Db.data?.contentHash)

        assertEquals(StorageFileStatus.READY, (file2Db as AppResult.Success).data?.status)
        assertEquals(700L, file2Db.data?.fileSizeBytes)
        assertEquals("new-hash-2", file2Db.data?.contentHash)
    }

    @Test
    fun `getStorageFilesByStatus returns files matching specified status`() = runBlocking {
        val file1 = createSampleFile(status = StorageFileStatus.DELETED)
        val file2 = createSampleFile(status = StorageFileStatus.READY)

        suspendTransaction {
            repository.createStorageFile(file1)
            repository.createStorageFile(file2)
        }

        val result = suspendTransaction {
            repository.getStorageFilesByStatus(status = StorageFileStatus.DELETED, limit = 10)
        }

        assertTrue(result is AppResult.Success)
        val files = (result as AppResult.Success).data
        assertEquals(1, files.size)
        assertEquals(file1.id, files.first().id)
    }

    @Test
    fun `hardDeleteStorageFilesBatch removes specified records`() = runBlocking {
        val file1 = createSampleFile()
        val file2 = createSampleFile()

        suspendTransaction {
            repository.createStorageFile(file1)
            repository.createStorageFile(file2)
        }

        suspendTransaction {
            repository.hardDeleteStorageFilesBatch(listOf(file1.id))
        }

        val file1Db = suspendTransaction { repository.getStorageFileById(file1.id) }
        val file2Db = suspendTransaction { repository.getStorageFileById(file2.id) }

        assertEquals(null, (file1Db as AppResult.Success).data)
        assertEquals(file2.id, (file2Db as AppResult.Success).data?.id)
    }
}
