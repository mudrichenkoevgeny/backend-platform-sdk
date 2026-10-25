package io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.storage

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeleteFilesUseCaseTest {

    private val storageManager = mockk<StorageManager>()
    private val useCase = DeleteFilesUseCase(storageManager)

    private val context = createTestAuthenticatedRequestContext()
    private val currentUserId = context.userId
    private val fileId1 = FileId.generate()
    private val fileId2 = FileId.generate()
    private val fileIds = listOf(fileId1, fileId2)

    @Test
    fun `returns success immediately when fileIds list is empty`() = runTest {
        val result = useCase(emptyList(), context)

        assertEquals(AppResult.Success(Unit), result)
        coVerify(exactly = 0) { storageManager.deleteSelfStorageFilesBatch(any(), any()) }
    }

    @Test
    fun `successfully deletes file batch for current user`() = runTest {
        coEvery {
            storageManager.deleteSelfStorageFilesBatch(
                ownerUserId = currentUserId.asHexDashString(),
                fileIds = fileIds
            )
        } returns AppResult.Success(Unit)

        val result = useCase(fileIds, context)

        assertEquals(AppResult.Success(Unit), result)
        coVerify(exactly = 1) {
            storageManager.deleteSelfStorageFilesBatch(
                ownerUserId = currentUserId.asHexDashString(),
                fileIds = fileIds
            )
        }
    }

    @Test
    fun `returns error when storageManager batch delete fails`() = runTest {
        val expectedError = CommonError.Internal(RuntimeException("test"))

        coEvery {
            storageManager.deleteSelfStorageFilesBatch(
                ownerUserId = currentUserId.asHexDashString(),
                fileIds = fileIds
            )
        } returns AppResult.Error(expectedError)

        val result = useCase(fileIds, context)

        assertTrue(result is AppResult.Error && result.error == expectedError)
    }
}
