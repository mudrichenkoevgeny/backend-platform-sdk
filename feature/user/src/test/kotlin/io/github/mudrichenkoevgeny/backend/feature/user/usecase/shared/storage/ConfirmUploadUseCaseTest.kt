package io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.storage

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFilePrivate
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.status.StorageFileStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Clock

class ConfirmUploadUseCaseTest {

    private val storageManager = mockk<StorageManager>()
    private val useCase = ConfirmUploadUseCase(storageManager)

    private val context = createTestAuthenticatedRequestContext()
    private val currentUserId = context.userId
    private val fileId = FileId.generate()

    private val filePrivate = StorageFilePrivate(
        id = fileId,
        ownerUserId = currentUserId.asHexDashString(),
        fileName = "avatar.png",
        fileSizeBytes = 500L,
        mimeType = "image/png",
        category = StorageCategory.IMAGE,
        visibility = StorageFileVisibility.PUBLIC,
        status = StorageFileStatus.READY,
        targetScope = "avatar",
        fileUrl = "https://cdn.example.com/avatar.png",
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    @Test
    fun `successfully confirms upload`() = runTest {
        coEvery {
            storageManager.confirmUpload(
                ownerUserId = currentUserId.asHexDashString(),
                fileId = fileId
            )
        } returns AppResult.Success(filePrivate)

        val result = useCase(fileId, context)

        assertEquals(AppResult.Success(filePrivate), result)
    }

    @Test
    fun `returns error when confirmUpload fails`() = runTest {
        val expectedError = CommonError.Internal(RuntimeException("test"))

        coEvery {
            storageManager.confirmUpload(
                ownerUserId = currentUserId.asHexDashString(),
                fileId = fileId
            )
        } returns AppResult.Error(expectedError)

        val result = useCase(fileId, context)

        assertTrue(result is AppResult.Error && result.error == expectedError)
    }
}
