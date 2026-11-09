package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user

import io.github.mudrichenkoevgeny.backend.core.common.pagination.ListingQueryParams
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.UserListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.ListingDefaultValues
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortBy
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.user.toUserSummary
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserInternal
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ManagementGetUsersUseCaseTest {
    private val userManager = mockk<UserManager>()
    private val useCase = ManagementGetUsersUseCase(userManager)

    private val context = createTestAuthenticatedRequestContext()
    private val managerId = context.userId

    private val defaultQueryParams = UserListQueryParams(
        listing = ListingQueryParams(
            pageParams = PageParams(),
            sortBy = UserSortBy.DEFAULT,
            sortOrder = SortOrder.DEFAULT
        )
    )

    @BeforeEach
    fun setUp() {
        mockkStatic(UserSessionId::class)
        mockkStatic(UserId::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UserSessionId::class)
        unmockkStatic(UserId::class)
    }

    @Test
    fun `successfully returns paged users when manager is active`() = runTest {
        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.ACTIVE
        )
        val userInternal = createTestUserInternal()
        val pagedInternalResult = PagedResult(
            items = listOf(userInternal),
            totalCount = 1,
            pageNumber = ListingDefaultValues.Pagination.PAGE_NUMBER,
            pageSize = ListingDefaultValues.Pagination.PAGE_SIZE,
            totalPages = 1
        )
        val expectedPagedResult = PagedResult(
            items = listOf(userInternal.toUserSummary()),
            totalCount = 1,
            pageNumber = ListingDefaultValues.Pagination.PAGE_NUMBER,
            pageSize = ListingDefaultValues.Pagination.PAGE_SIZE,
            totalPages = 1
        )

        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery {
            userManager.getUsersInternalPage(
                managementUserPermissionCodes = any(),
                queryParams = any()
            )
        } returns AppResult.Success(pagedInternalResult)

        val result = useCase(
            queryParams = defaultQueryParams,
            authenticatedRequestContext = context
        )

        assertEquals(
            AppResult.Success(expectedPagedResult),
            result
        )
    }

    @Test
    fun `returns error when manager is not active`() = runTest {
        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.BANNED
        )

        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)

        val result = useCase(
            queryParams = defaultQueryParams,
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserIllegalAccountStatus)
    }

    @Test
    fun `returns error when manager not found`() = runTest {
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(null)

        val result = useCase(
            queryParams = defaultQueryParams,
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserForbidden)
    }
}
