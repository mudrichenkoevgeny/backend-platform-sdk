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

class DeleteFileUseCaseTest {

    private val storageManager = mockk<StorageManager>()
    private val useCase = DeleteFileUseCase(storageManager)

    private val context = createTestAuthenticatedRequestContext()
    private val currentUserId = context.userId
    private val fileId = FileId.generate()

    @Test
    fun `successfully deletes file for current user`() = runTest {
        coEvery {
            storageManager.deleteSelfStorageFile(
                ownerUserId = currentUserId.asHexDashString(),
                fileId = fileId
            )
        } returns AppResult.Success(Unit)

        val result = useCase(fileId, context)

        assertEquals(AppResult.Success(Unit), result)
        coVerify(exactly = 1) {
            storageManager.deleteSelfStorageFile(
                ownerUserId = currentUserId.asHexDashString(),
                fileId = fileId
            )
        }
    }

    @Test
    fun `returns error when storageManager delete fails`() = runTest {
        val expectedError = CommonError.Internal(RuntimeException("test"))

        coEvery {
            storageManager.deleteSelfStorageFile(
                ownerUserId = currentUserId.asHexDashString(),
                fileId = fileId
            )
        } returns AppResult.Error(expectedError)

        val result = useCase(fileId, context)

        assertTrue(result is AppResult.Error && result.error == expectedError)
    }
}
