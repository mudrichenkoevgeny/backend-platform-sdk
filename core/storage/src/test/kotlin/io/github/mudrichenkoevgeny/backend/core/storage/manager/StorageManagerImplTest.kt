package io.github.mudrichenkoevgeny.backend.core.storage.manager

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.util.createTestDataSource
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.createTestStorageConfig
import io.github.mudrichenkoevgeny.backend.core.storage.database.repository.StorageFilesRepository
import io.github.mudrichenkoevgeny.backend.core.storage.domain.model.S3ObjectMetadata
import io.github.mudrichenkoevgeny.backend.core.storage.service.StorageService
import io.github.mudrichenkoevgeny.backend.core.storage.settings.provider.StorageSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFileInternal
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.status.StorageFileStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Clock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StorageManagerImplTest {

    private val dataSource = createTestDataSource("storage_manager_test")
    private val filesRepository = mockk<StorageFilesRepository>()
    private val settingsProvider = mockk<StorageSettingsProvider>()
    private val storageService = mockk<StorageService>()
    private val storageConfig = createTestStorageConfig(maintenanceBatchSize = 10)

    private val manager = StorageManagerImpl(
        filesRepository = filesRepository,
        settingsProvider = settingsProvider,
        storageService = storageService,
        storageConfig = storageConfig
    )

    @BeforeAll
    fun setupAll() {
        Database.connect(dataSource)
    }

    private fun createSampleFileInternal(
        id: FileId = FileId.generate(),
        status: StorageFileStatus = StorageFileStatus.DELETED,
        s3Key: String = "uploads/${id.value}"
    ) = StorageFileInternal(
        id = id,
        ownerUserId = "usr_123",
        fileName = "test.png",
        fileSizeBytes = 100L,
        mimeType = "image/png",
        category = StorageCategory.IMAGE,
        visibility = StorageFileVisibility.PUBLIC,
        status = status,
        targetScope = "avatar",
        contentHash = "hash",
        s3Bucket = "files",
        s3Key = s3Key,
        createdAt = Clock.System.now(),
        updatedAt = Clock.System.now()
    )

    @Test
    fun `cleanupDeletedStorageFiles deletes binaries from storage and removes DB records`() = runTest {
        val file1 = createSampleFileInternal(id = FileId.generate(), status = StorageFileStatus.DELETED)
        val file2 = createSampleFileInternal(id = FileId.generate(), status = StorageFileStatus.DELETED)

        coEvery {
            filesRepository.getStorageFilesByStatus(StorageFileStatus.DELETED, 10)
        } returns AppResult.Success(listOf(file1, file2))

        coEvery { storageService.deleteBatch(listOf(file1.s3Key, file2.s3Key)) } returns AppResult.Success(Unit)
        coEvery { filesRepository.hardDeleteStorageFilesBatch(listOf(file1.id, file2.id)) } returns AppResult.Success(Unit)

        val result = manager.cleanupDeletedStorageFiles()

        assertTrue(result is AppResult.Success)
        assertEquals(2, (result as AppResult.Success).data)

        coVerify(exactly = 1) {
            storageService.deleteBatch(listOf(file1.s3Key, file2.s3Key))
            filesRepository.hardDeleteStorageFilesBatch(listOf(file1.id, file2.id))
        }
    }

    @Test
    fun `processExpiredPendingUploads updates ready files and deletes abandoned files`() = runTest {
        val readyFile = createSampleFileInternal(id = FileId.generate(), status = StorageFileStatus.PENDING_UPLOAD)
        val abandonedFile = createSampleFileInternal(id = FileId.generate(), status = StorageFileStatus.PENDING_UPLOAD)

        every { settingsProvider.getPresignedUrlExpirationSeconds() } returns 900

        coEvery {
            filesRepository.getExpiredPendingUploadFiles(any(), any())
        } returns AppResult.Success(listOf(readyFile, abandonedFile))

        coEvery {
            storageService.getObjectMetadata(readyFile.s3Key)
        } returns AppResult.Success(S3ObjectMetadata(sizeBytes = 500L, eTag = "etag123"))

        coEvery {
            storageService.getObjectMetadata(abandonedFile.s3Key)
        } returns AppResult.Error(CommonError.Internal(Exception("Not found")))

        coEvery {
            filesRepository.updateStorageFilesBatch(any())
        } returns AppResult.Success(Unit)

        coEvery {
            filesRepository.hardDeleteStorageFilesBatch(any())
        } returns AppResult.Success(Unit)

        val result = manager.processExpiredPendingUploads()

        assertTrue(result is AppResult.Success)
        assertEquals(2, (result as AppResult.Success).data)

        val updatesSlot = slot<List<StorageFileInternal>>()
        coVerify(exactly = 1) {
            filesRepository.updateStorageFilesBatch(capture(updatesSlot))
        }
        coVerify(exactly = 1) {
            filesRepository.hardDeleteStorageFilesBatch(listOf(abandonedFile.id))
        }

        assertEquals(1, updatesSlot.captured.size)
        assertEquals(readyFile.id, updatesSlot.captured.first().id)
        assertEquals(StorageFileStatus.READY, updatesSlot.captured.first().status)
    }

    @Test
    fun `getSelfStorageFilePrivate auto-heals pending upload file to READY if S3 confirms metadata`() = runTest {
        val userId = "usr_123"
        val fileId = FileId.generate()
        val pendingFile = createSampleFileInternal(id = fileId, status = StorageFileStatus.PENDING_UPLOAD)
        val readyFile = pendingFile.copy(status = StorageFileStatus.READY, fileSizeBytes = 500L, contentHash = "etag123")

        coEvery { filesRepository.getStorageFileById(fileId) } returns AppResult.Success(pendingFile)
        coEvery { storageService.getObjectMetadata(pendingFile.s3Key) } returns AppResult.Success(S3ObjectMetadata(sizeBytes = 500L, eTag = "etag123"))
        coEvery {
            filesRepository.updateStorageFile(
                fileId = fileId,
                status = UpdateField.Set(StorageFileStatus.READY),
                fileSizeBytes = UpdateField.Set(500L),
                contentHash = UpdateField.Set("etag123")
            )
        } returns AppResult.Success(readyFile)

        every { storageService.getUrl(pendingFile.s3Key) } returns AppResult.Success("https://cdn.example.com/files/test.png")

        val result = manager.getSelfStorageFilePrivate(ownerUserId = userId, fileId = fileId)

        assertTrue(result is AppResult.Success)
        val filePrivate = (result as AppResult.Success).data
        assertEquals(StorageFileStatus.READY, filePrivate?.status)
        assertEquals(500L, filePrivate?.fileSizeBytes)
    }
}
