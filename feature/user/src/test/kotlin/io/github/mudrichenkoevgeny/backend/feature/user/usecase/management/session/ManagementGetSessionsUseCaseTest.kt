package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.session

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
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

class ManagementGetSessionsUseCaseTest {

    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()

    private val useCase = ManagementGetSessionsUseCase(
        userManager = userManager,
        sessionManager = sessionManager
    )

    private val defaultContext = AuthenticatedRequestContext(
        traceId = null,
        userId = UserId.generate(),
        userRole = UserRole.ADMIN,
        sessionId = UserSessionId.generate(),
        clientInfo = mockk(relaxed = true)
    )

    @Test
    fun `successfully retrieves paginated sessions`() = runTest {
        val managementUserId = defaultContext.userId
        val pageParams = PageParams(page = 1, size = 20)
        val pagedResult = PagedResult<UserSession>(
            items = emptyList(),
            totalCount = 0,
            pageNumber = 1,
            pageSize = 10,
            totalPages = 1
        )

        val managementUser = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { permissionCodes } returns emptySet()
        }

        coEvery { userManager.getUserByIdForSelf(managementUserId) } returns AppResult.Success(managementUser)
        coEvery {
            sessionManager.getSessionsPageForManagement(
                managementUserPermissionCodes = any(),
                pageParams = pageParams,
                sortBy = any(),
                sortOrder = any(),
                userIds = any(),
                userRoles = any(),
                identifiers = any(),
                identifierIds = any(),
                identifierAuthProviders = any(),
                clientTypes = any(),
                userAgents = any(),
                ipAddresses = any(),
                languages = any(),
                deviceIds = any(),
                deviceNames = any(),
                appVersions = any(),
                operationSystemVersions = any()
            )
        } returns AppResult.Success(pagedResult)

        val result = useCase(
            pageParams = pageParams,
            sortBy = UserSortValues.UserSessionSortBy.CREATED_AT,
            sortOrder = SortOrder.DESC,
            userIds = emptyList(),
            userRoles = emptyList(),
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
            authenticatedRequestContext = defaultContext
        )

        assertEquals(AppResult.Success(pagedResult), result)
    }

    @Test
    fun `returns error when management user is not active`() = runTest {
        val managementUserId = defaultContext.userId
        val managementUser = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.BANNED
        }

        coEvery { userManager.getUserByIdForSelf(managementUserId) } returns AppResult.Success(managementUser)

        val result = useCase(
            pageParams = PageParams(0, 20),
            sortBy = UserSortValues.UserSessionSortBy.CREATED_AT,
            sortOrder = SortOrder.DESC,
            userIds = emptyList(),
            userRoles = emptyList(),
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
            authenticatedRequestContext = defaultContext
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserIllegalAccountStatus)
    }

    @Test
    fun `returns error when management user not found`() = runTest {
        coEvery { userManager.getUserByIdForSelf(any()) } returns AppResult.Success(null)

        val result = useCase(
            pageParams = PageParams(0, 20),
            sortBy = UserSortValues.UserSessionSortBy.CREATED_AT,
            sortOrder = SortOrder.DESC,
            userIds = emptyList(),
            userRoles = emptyList(),
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
            authenticatedRequestContext = defaultContext
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserForbidden)
    }
}