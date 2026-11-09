package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier

import io.github.mudrichenkoevgeny.backend.core.common.pagination.ListingQueryParams
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.ManagementIdentifierListQueryParams
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierSummary
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserIdentifierSortBy
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.common.domain.model.listing.createTestPagedResult
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserInternal
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManagementGetIdentifiersUseCaseTest {
    private val userManager = mockk<UserManager>()
    private val identifierManager = mockk<IdentifierManager>()

    private val useCase = ManagementGetIdentifiersUseCase(
        userManager = userManager,
        identifierManager = identifierManager
    )

    private val defaultQueryParams = ManagementIdentifierListQueryParams(
        listing = ListingQueryParams(
            pageParams = PageParams(),
            sortBy = UserIdentifierSortBy.DEFAULT,
            sortOrder = SortOrder.DEFAULT
        )
    )

    @Test
    fun `successfully returns paginated identifiers for management`() = runTest {
        val managerId = UserId.generate()
        val context = createTestAuthenticatedRequestContext(userId = managerId)
        val permissions = setOf(PermissionCode("admin.access"))
        val pagedResult = createTestPagedResult<UserIdentifierSummary>()

        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.ACTIVE,
            permissionCodes = permissions
        )

        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery {
            identifierManager.getUserIdentifiersSummaryPage(
                managementUserPermissionCodes = permissions,
                queryParams = any()
            )
        } returns AppResult.Success(pagedResult)

        val result = useCase(
            queryParams = defaultQueryParams,
            authenticatedRequestContext = context
        )

        assertEquals(
            AppResult.Success(pagedResult),
            result
        )
    }

    @Test
    fun `returns error when manager is not active`() = runTest {
        val managerId = UserId.generate()
        val context = createTestAuthenticatedRequestContext(userId = managerId)
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
    fun `returns error when manager user is not found`() = runTest {
        val managerId = UserId.generate()
        val context = createTestAuthenticatedRequestContext(userId = managerId)

        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(null)

        val result = useCase(
            queryParams = defaultQueryParams,
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserForbidden)
    }
}
