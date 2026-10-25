package io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.storage

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.presigned.PresignedUrl
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserInternal
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Clock

class GeneratePresignedUploadUrlUseCaseTest {

    private val userManager = mockk<UserManager>()
    private val storageManager = mockk<StorageManager>()

    private val useCase = GeneratePresignedUploadUrlUseCase(
        userManager = userManager,
        storageManager = storageManager
    )

    private val context = createTestAuthenticatedRequestContext()
    private val currentUserId = context.userId

    private val presignedUrl = PresignedUrl(
        fileId = FileId.generate(),
        uploadUrl = "https://s3.example.com/upload",
        fileUrl = "https://cdn.example.com/file",
        expiresAt = Clock.System.now()
    )

    @Test
    fun `successfully generates presigned upload url`() = runTest {
        val user = createTestUserInternal(id = currentUserId, role = UserRole.USER)

        coEvery { userManager.getUserInternalById(currentUserId) } returns AppResult.Success(user)
        coEvery {
            storageManager.initiateFileUpload(
                ownerUserId = currentUserId.asHexDashString(),
                isManagementUser = false,
                fileName = "avatar.png",
                fileSizeBytes = 500L,
                mimeType = "image/png",
                category = StorageCategory.IMAGE,
                visibility = StorageFileVisibility.PUBLIC,
                targetScope = "avatar",
                customStorageQuotaBytes = null,
                customMaxUploadSizeBytes = null,
                customMaxPendingUploads = null
            )
        } returns AppResult.Success(presignedUrl)

        val result = useCase(
            fileName = "avatar.png",
            fileSizeBytes = 500L,
            mimeType = "image/png",
            storageCategory = StorageCategory.IMAGE,
            storageFileVisibility = StorageFileVisibility.PUBLIC,
            targetScope = "avatar",
            authenticatedRequestContext = context
        )

        assertEquals(AppResult.Success(presignedUrl), result)
    }

    @Test
    fun `returns UserForbidden error when current user is not found`() = runTest {
        coEvery { userManager.getUserInternalById(currentUserId) } returns AppResult.Success(null)

        val result = useCase(
            fileName = "avatar.png",
            fileSizeBytes = 500L,
            mimeType = "image/png",
            storageCategory = StorageCategory.IMAGE,
            storageFileVisibility = StorageFileVisibility.PUBLIC,
            targetScope = "avatar",
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserForbidden)
    }

    @Test
    fun `identifies management user role correctly when initiating upload`() = runTest {
        val adminUser = createTestUserInternal(id = currentUserId, role = UserRole.ADMIN)

        coEvery { userManager.getUserInternalById(currentUserId) } returns AppResult.Success(adminUser)
        coEvery {
            storageManager.initiateFileUpload(
                ownerUserId = currentUserId.asHexDashString(),
                isManagementUser = true,
                fileName = "avatar.png",
                fileSizeBytes = 500L,
                mimeType = "image/png",
                category = StorageCategory.IMAGE,
                visibility = StorageFileVisibility.PUBLIC,
                targetScope = "avatar",
                customStorageQuotaBytes = null,
                customMaxUploadSizeBytes = null,
                customMaxPendingUploads = null
            )
        } returns AppResult.Success(presignedUrl)

        val result = useCase(
            fileName = "avatar.png",
            fileSizeBytes = 500L,
            mimeType = "image/png",
            storageCategory = StorageCategory.IMAGE,
            storageFileVisibility = StorageFileVisibility.PUBLIC,
            targetScope = "avatar",
            authenticatedRequestContext = context
        )

        assertEquals(AppResult.Success(presignedUrl), result)
    }
}
