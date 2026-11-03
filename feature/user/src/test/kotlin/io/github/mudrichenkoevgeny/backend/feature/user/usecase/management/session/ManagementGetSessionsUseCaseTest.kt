package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.session

import io.github.mudrichenkoevgeny.backend.core.common.pagination.ListingQueryParams
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.ManagementSessionListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.ListingDefaultValues
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSessionSortBy
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionSummary
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserInternal
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManagementGetSessionsUseCaseTest {

    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()

    private val useCase = ManagementGetSessionsUseCase(
        userManager = userManager,
        sessionManager = sessionManager
    )

    private val defaultContext = createTestAuthenticatedRequestContext()

    private val defaultQueryParams = ManagementSessionListQueryParams(
        listing = ListingQueryParams(
            pageParams = PageParams(),
            sortBy = UserSessionSortBy.DEFAULT,
            sortOrder = SortOrder.DEFAULT
        )
    )

    @Test
    fun `successfully retrieves paginated sessions`() = runTest {
        val managementUserId = defaultContext.userId
        val pagedResult = PagedResult<UserSessionSummary>(
            items = emptyList(),
            totalCount = 0,
            pageNumber = ListingDefaultValues.Pagination.PAGE_NUMBER,
            pageSize = ListingDefaultValues.Pagination.PAGE_SIZE,
            totalPages = 1
        )

        val managementUser = createTestUserInternal(
            id = managementUserId,
            accountStatus = UserAccountStatus.ACTIVE,
            permissionCodes = emptySet()
        )

        coEvery { userManager.getUserInternalById(managementUserId) } returns AppResult.Success(managementUser)
        coEvery {
            sessionManager.getUserSessionsSummaryPage(
                managementUserPermissionCodes = any(),
                queryParams = any()
            )
        } returns AppResult.Success(pagedResult)

        val result = useCase(
            queryParams = defaultQueryParams,
            authenticatedRequestContext = defaultContext
        )

        assertEquals(AppResult.Success(pagedResult), result)
    }

    @Test
    fun `returns error when management user is not active`() = runTest {
        val managementUserId = defaultContext.userId
        val managementUser = createTestUserInternal(
            id = managementUserId,
            accountStatus = UserAccountStatus.BANNED
        )

        coEvery { userManager.getUserInternalById(managementUserId) } returns AppResult.Success(managementUser)

        val result = useCase(
            queryParams = defaultQueryParams,
            authenticatedRequestContext = defaultContext
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserIllegalAccountStatus)
    }

    @Test
    fun `returns error when management user not found`() = runTest {
        coEvery { userManager.getUserInternalById(any()) } returns AppResult.Success(null)

        val result = useCase(
            queryParams = defaultQueryParams,
            authenticatedRequestContext = defaultContext
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserForbidden)
    }
}
