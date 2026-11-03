package io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.identifier

import io.github.mudrichenkoevgeny.backend.core.common.pagination.ListingQueryParams
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.SelfIdentifierListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierSummary
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserIdentifierSortBy
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.common.domain.model.listing.createTestPagedResult
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserInternal
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetIdentifiersUseCaseTest {

    private val userManager = mockk<UserManager>()
    private val identifierManager = mockk<IdentifierManager>()

    private val useCase = GetIdentifiersUseCase(
        userManager = userManager,
        identifierManager = identifierManager
    )

    private val defaultQueryParams = SelfIdentifierListQueryParams(
        listing = ListingQueryParams(
            pageParams = PageParams(),
            sortBy = UserIdentifierSortBy.DEFAULT,
            sortOrder = SortOrder.DEFAULT
        )
    )

    @Test
    fun `successfully returns paginated identifiers`() = runTest {
        val context = createTestAuthenticatedRequestContext()
        val userInternal = createTestUserInternal(id = context.userId, accountStatus = UserAccountStatus.ACTIVE)
        val pagedResult = createTestPagedResult<UserIdentifierSummary>()

        coEvery {
            userManager.getUserInternalById(context.userId)
        } returns AppResult.Success(userInternal)

        coEvery {
            identifierManager.getUserIdentifiersSummaryPage(
                userId = context.userId,
                queryParams = any()
            )
        } returns AppResult.Success(pagedResult)

        val result = useCase(
            queryParams = defaultQueryParams,
            authenticatedRequestContext = context
        )

        assertEquals(AppResult.Success(pagedResult), result)
    }

    @Test
    fun `returns error when account status is not allowed`() = runTest {
        val context = createTestAuthenticatedRequestContext()
        val userInternal = createTestUserInternal(id = context.userId, accountStatus = UserAccountStatus.PENDING_DELETION)

        coEvery { userManager.getUserInternalById(context.userId) } returns AppResult.Success(userInternal)

        val result = useCase(
            queryParams = defaultQueryParams,
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is UserError.UserForbidden)
        assertEquals(context.userId, (error as UserError.UserForbidden).userId)
    }

    @Test
    fun `returns success when account status is READ_ONLY`() = runTest {
        val context = createTestAuthenticatedRequestContext()
        val userInternal = createTestUserInternal(id = context.userId, accountStatus = UserAccountStatus.READ_ONLY)
        val pagedResult = createTestPagedResult<UserIdentifierSummary>()

        coEvery { userManager.getUserInternalById(context.userId) } returns AppResult.Success(userInternal)
        coEvery {
            identifierManager.getUserIdentifiersSummaryPage(
                userId = any(),
                queryParams = any()
            )
        } returns AppResult.Success(pagedResult)

        val result = useCase(
            queryParams = defaultQueryParams,
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Success)
    }
}
