package io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.session

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.common.domain.model.listing.createTestPagedResult
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserInternal
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.session.GetSessionsUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionSummary
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetSessionsUseCaseTest {

    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()

    private val useCase = GetSessionsUseCase(
        userManager = userManager,
        sessionManager = sessionManager
    )

    private val context = createTestAuthenticatedRequestContext(userRole = UserRole.USER)
    private val userId = context.userId

    private val defaultPageParams = PageParams()

    @Test
    fun `successfully returns paged sessions when user is active`() = runTest {
        val userInternal = createTestUserInternal(id = userId, accountStatus = UserAccountStatus.ACTIVE)
        val pagedResult = createTestPagedResult<UserSessionSummary>()

        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Success(userInternal)
        coEvery {
            sessionManager.getUserSessionsSummaryPage(
                userId = userId,
                pageParams = defaultPageParams,
                sortBy = UserSortValues.UserSessionSortBy.DEFAULT,
                sortOrder = SortOrder.DEFAULT,
                identifiers = emptyList(),
                identifierIds = emptyList(),
                identifierAuthProviders = emptyList(),
                clientTypes = emptyList(),
                userAgents = emptyList(),
                ipAddresses = emptyList(),
                languages = emptyList(),
                deviceIds = emptyList(),
                deviceNames = emptyList(),
                appVersions = emptyList(),
                operationSystemVersions = emptyList()
            )
        } returns AppResult.Success(pagedResult)

        val result = useCase(
            pageParams = defaultPageParams,
            sortBy = UserSortValues.UserSessionSortBy.DEFAULT,
            sortOrder = SortOrder.DEFAULT,
            identifiers = emptyList(),
            identifierIds = emptyList(),
            identifierAuthProviders = emptyList(),
            clientTypes = emptyList(),
            userAgents = emptyList(),
            ipAddresses = emptyList(),
            languages = emptyList(),
            deviceIds = emptyList(),
            deviceNames = emptyList(),
            appVersions = emptyList(),
            operationSystemVersions = emptyList(),
            authenticatedRequestContext = context
        )

        assertEquals(AppResult.Success(pagedResult), result)
    }

    @Test
    fun `returns forbidden error when account status is not allowed`() = runTest {
        val userInternal = createTestUserInternal(id = userId, accountStatus = UserAccountStatus.PENDING_DELETION)

        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Success(userInternal)

        val result = useCase(
            pageParams = defaultPageParams,
            sortBy = UserSortValues.UserSessionSortBy.DEFAULT,
            sortOrder = SortOrder.DEFAULT,
            identifiers = emptyList(),
            identifierIds = emptyList(),
            identifierAuthProviders = emptyList(),
            clientTypes = emptyList(),
            userAgents = emptyList(),
            ipAddresses = emptyList(),
            languages = emptyList(),
            deviceIds = emptyList(),
            deviceNames = emptyList(),
            appVersions = emptyList(),
            operationSystemVersions = emptyList(),
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserForbidden)
    }

    @Test
    fun `returns error when user manager fails`() = runTest {
        val error = UserError.UserNotFound(userId)
        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Error(error)

        val result = useCase(
            pageParams = defaultPageParams,
            sortBy = UserSortValues.UserSessionSortBy.DEFAULT,
            sortOrder = SortOrder.DEFAULT,
            identifiers = emptyList(),
            identifierIds = emptyList(),
            identifierAuthProviders = emptyList(),
            clientTypes = emptyList(),
            userAgents = emptyList(),
            ipAddresses = emptyList(),
            languages = emptyList(),
            deviceIds = emptyList(),
            deviceNames = emptyList(),
            appVersions = emptyList(),
            operationSystemVersions = emptyList(),
            authenticatedRequestContext = context
        )

        assertEquals(AppResult.Error(error), result)
    }
}
