package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storage

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFilePrivate
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.status.StorageFileStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.permission.StoragePermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserInternal
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Clock

class ManagementGetFileUseCaseTest {

    private val storageManager = mockk<StorageManager>()
    private val userManager = mockk<UserManager>()

    private val useCase = ManagementGetFileUseCase(
        storageManager = storageManager,
        userManager = userManager
    )

    private val context = createTestAuthenticatedRequestContext()
    private val managementUserId = context.userId
    private val fileId = FileId.generate()

    private val filePrivate = StorageFilePrivate(
        id = fileId,
        ownerUserId = "user-123",
        fileName = "document.pdf",
        fileSizeBytes = 1000L,
        mimeType = "application/pdf",
        category = StorageCategory.DOCUMENT,
        visibility = StorageFileVisibility.PUBLIC,
        status = StorageFileStatus.READY,
        targetScope = "doc",
        fileUrl = "https://cdn.example.com/document.pdf",
        createdAt = Clock.System.now(),
        updatedAt = null
    )

    @Test
    fun `returns file when user is authorized and has view metadata permission`() = runTest {
        val managementUser = createTestUserInternal(
            id = managementUserId,
            role = UserRole.ADMIN,
            accountStatus = UserAccountStatus.ACTIVE,
            permissionCodes = setOf(StoragePermissionCode.STORAGE_VIEW_OTHER_USERS_FILES)
        )

        coEvery { userManager.getUserInternalById(managementUserId) } returns AppResult.Success(managementUser)
        coEvery {
            storageManager.getStorageFilePrivate(
                managementUserPermissionCodes = managementUser.permissionCodes,
                fileId = fileId
            )
        } returns AppResult.Success(filePrivate)

        val result = useCase(fileId, context)

        assertEquals(AppResult.Success(filePrivate), result)
    }

    @Test
    fun `returns UserForbidden error when management user is not found`() = runTest {
        coEvery { userManager.getUserInternalById(managementUserId) } returns AppResult.Success(null)

        val result = useCase(fileId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserForbidden)
    }

    @Test
    fun `returns UserMissingPermissions error when user lacks view metadata permission`() = runTest {
        val managementUser = createTestUserInternal(
            id = managementUserId,
            role = UserRole.ADMIN,
            accountStatus = UserAccountStatus.ACTIVE,
            permissionCodes = emptySet()
        )

        coEvery { userManager.getUserInternalById(managementUserId) } returns AppResult.Success(managementUser)

        val result = useCase(fileId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserMissingPermissions)
    }

    @Test
    fun `returns NotFound error when file is not found`() = runTest {
        val managementUser = createTestUserInternal(
            id = managementUserId,
            role = UserRole.ADMIN,
            accountStatus = UserAccountStatus.ACTIVE,
            permissionCodes = setOf(StoragePermissionCode.STORAGE_VIEW_OTHER_USERS_FILES)
        )

        coEvery { userManager.getUserInternalById(managementUserId) } returns AppResult.Success(managementUser)
        coEvery {
            storageManager.getStorageFilePrivate(
                managementUserPermissionCodes = managementUser.permissionCodes,
                fileId = fileId
            )
        } returns AppResult.Success(null)

        val result = useCase(fileId, context)

        assertTrue(result is AppResult.Error && result.error is CommonError.NotFound)
    }
}
