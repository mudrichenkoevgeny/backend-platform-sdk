package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storage

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFileSummary
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.listing.StorageSortValues
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

class ManagementGetFilesUseCaseTest {

    private val storageManager = mockk<StorageManager>()
    private val userManager = mockk<UserManager>()

    private val useCase = ManagementGetFilesUseCase(
        storageManager = storageManager,
        userManager = userManager
    )

    private val context = createTestAuthenticatedRequestContext()
    private val managementUserId = context.userId

    private val fileSummary = StorageFileSummary(
        id = FileId.generate(),
        fileName = "document.pdf",
        fileSizeBytes = 1000L,
        mimeType = "application/pdf",
        category = StorageCategory.DOCUMENT,
        fileUrl = "https://cdn.example.com/document.pdf"
    )

    private val pagedResult = PagedResult(
        items = listOf(fileSummary),
        totalCount = 1L,
        pageNumber = 1,
        pageSize = 20,
        totalPages = 1
    )

    @Test
    fun `returns paged files when user is authorized and has view metadata permission`() = runTest {
        val managementUser = createTestUserInternal(
            id = managementUserId,
            role = UserRole.ADMIN,
            accountStatus = UserAccountStatus.ACTIVE,
            permissionCodes = setOf(StoragePermissionCode.STORAGE_VIEW_OTHER_USERS_FILES)
        )
        val pageParams = PageParams()

        coEvery { userManager.getUserInternalById(managementUserId) } returns AppResult.Success(managementUser)
        coEvery {
            storageManager.getStorageFilesSummaryPage(
                managementUserPermissionCodes = managementUser.permissionCodes,
                pageParams = pageParams,
                sortBy = StorageSortValues.StorageFileSortBy.DEFAULT,
                sortOrder = SortOrder.DEFAULT,
                ownerUserIds = emptyList(),
                categories = emptyList(),
                visibilities = emptyList(),
                statuses = emptyList(),
                mimeTypes = emptyList(),
                fileNames = emptyList()
            )
        } returns AppResult.Success(pagedResult)

        val result = useCase(context, pageParams)

        assertEquals(AppResult.Success(pagedResult), result)
    }

    @Test
    fun `returns UserForbidden error when management user is not found`() = runTest {
        coEvery { userManager.getUserInternalById(managementUserId) } returns AppResult.Success(null)

        val result = useCase(context)

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

        val result = useCase(context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserMissingPermissions)
    }
}
