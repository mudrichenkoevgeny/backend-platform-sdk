package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.session

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSession
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.every
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

    private val userId = UserId.generate()
    private val context = AuthenticatedRequestContext(
        traceId = null,
        userId = userId,
        userRole = UserRole.USER,
        sessionId = UserSessionId.generate(),
        clientInfo = ClientInfo()
    )

    private val defaultPageParams = PageParams(page = 1, size = 10)

    @Test
    fun `successfully returns paged sessions when user is active`() = runTest {
        val userInternal = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
        }
        val pagedResult = mockk<PagedResult<UserSession>>()

        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userInternal)
        coEvery {
            sessionManager.getSessionsPageForSelf(
                userId = userId,
                pageParams = defaultPageParams,
                sortBy = UserSortValues.UserSessionSortBy.CREATED_AT,
                sortOrder = SortOrder.DESC,
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
            sortBy = UserSortValues.UserSessionSortBy.CREATED_AT,
            sortOrder = SortOrder.DESC,
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
        val userInternal = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.PENDING_DELETION
        }

        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userInternal)

        val result = useCase(
            pageParams = defaultPageParams,
            sortBy = UserSortValues.UserSessionSortBy.CREATED_AT,
            sortOrder = SortOrder.DESC,
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
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Error(error)

        val result = useCase(
            pageParams = defaultPageParams,
            sortBy = UserSortValues.UserSessionSortBy.CREATED_AT,
            sortOrder = SortOrder.DESC,
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