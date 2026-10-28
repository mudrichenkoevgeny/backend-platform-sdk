package io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.storage

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.listing.StorageSortValues
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.storage.domain.model.file.createTestStorageFileSummary
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetFilesUseCaseTest {

    private val storageManager = mockk<StorageManager>()
    private val useCase = GetFilesUseCase(storageManager)

    private val context = createTestAuthenticatedRequestContext()
    private val currentUserId = context.userId

    private val fileSummary = createTestStorageFileSummary(
        id = FileId.generate(),
        fileName = "avatar.png",
        fileSizeBytes = 500L,
        mimeType = "image/png",
        category = StorageCategory.IMAGE,
        fileUrl = "https://cdn.example.com/avatar.png"
    )

    private val pagedResult = PagedResult(
        items = listOf(fileSummary),
        totalCount = 1L,
        pageNumber = 1,
        pageSize = 20,
        totalPages = 1
    )

    @Test
    fun `returns paged summary files for current user`() = runTest {
        val pageParams = PageParams()

        coEvery {
            storageManager.getSelfStorageFilesSummaryPage(
                ownerUserId = currentUserId.asHexDashString(),
                pageParams = pageParams,
                sortBy = StorageSortValues.StorageFileSortBy.DEFAULT,
                sortOrder = SortOrder.DEFAULT,
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
}
