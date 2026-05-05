package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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

    private fun createAuthContext(userId: UserId = UserId.generate()) = AuthenticatedRequestContext(
        traceId = null,
        userId = userId,
        userRole = UserRole.ADMIN,
        sessionId = UserSessionId.generate(),
        clientInfo = ClientInfo()
    )

    @Test
    fun `successfully retrieves paged identifiers`() = runTest {
        val managerId = UserId.generate()
        val context = createAuthContext(managerId)
        val pageParams = PageParams(page = 1, size = 10)
        val permissions = setOf(PermissionCode("test.permission"))

        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { permissionCodes } returns permissions
        }

        val pagedResult = mockk<PagedResult<UserIdentifier>>()

        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        coEvery {
            identifierManager.getIdentifiersPageForManagement(
                managementUserPermissionCodes = permissions,
                pageParams = pageParams,
                sortBy = UserSortValues.UserIdentifierSortBy.CREATED_AT,
                sortOrder = SortOrder.DESC,
                userIds = emptyList(),
                userAuthProviders = emptyList(),
                identifiers = emptyList()
            )
        } returns AppResult.Success(pagedResult)

        val result = useCase(
            pageParams = pageParams,
            sortBy = UserSortValues.UserIdentifierSortBy.CREATED_AT,
            sortOrder = SortOrder.DESC,
            userIds = emptyList(),
            userAuthProviders = emptyList(),
            identifiers = emptyList(),
            authenticatedRequestContext = context
        )

        assertEquals(AppResult.Success(pagedResult), result)
    }

    @Test
    fun `returns error when management user is not active`() = runTest {
        val managerId = UserId.generate()
        val context = createAuthContext(managerId)

        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.BANNED
        }

        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)

        val result = useCase(
            pageParams = PageParams(1, 10),
            sortBy = UserSortValues.UserIdentifierSortBy.CREATED_AT,
            sortOrder = SortOrder.DESC,
            userIds = emptyList(),
            userAuthProviders = emptyList(),
            identifiers = emptyList(),
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserIllegalAccountStatus)
        coVerify(exactly = 0) { identifierManager.getIdentifiersPageForManagement(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `returns error when management user not found`() = runTest {
        val managerId = UserId.generate()
        val context = createAuthContext(managerId)

        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(null)

        val result = useCase(
            pageParams = PageParams(1, 10),
            sortBy = UserSortValues.UserIdentifierSortBy.CREATED_AT,
            sortOrder = SortOrder.DESC,
            userIds = emptyList(),
            userAuthProviders = emptyList(),
            identifiers = emptyList(),
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserForbidden)
    }

    @Test
    fun `passes all filters and sort options to identifier manager`() = runTest {
        val managerId = UserId.generate()
        val context = createAuthContext(managerId)
        val filterUserIds = listOf(UserId.generate())
        val filterProviders = listOf(UserAuthProvider.EMAIL)
        val filterStrings = listOf("test")
        val permissions = setOf(PermissionCode("admin.access"))

        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { permissionCodes } returns permissions
        }

        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        coEvery {
            identifierManager.getIdentifiersPageForManagement(
                any(), any(), any(), any(), any(), any(), any()
            )
        } returns AppResult.Success(mockk())

        useCase(
            pageParams = PageParams(1, 10),
            sortBy = UserSortValues.UserIdentifierSortBy.UPDATED_AT,
            sortOrder = SortOrder.ASC,
            userIds = filterUserIds,
            userAuthProviders = filterProviders,
            identifiers = filterStrings,
            authenticatedRequestContext = context
        )

        coVerify {
            identifierManager.getIdentifiersPageForManagement(
                managementUserPermissionCodes = permissions,
                pageParams = any(),
                sortBy = UserSortValues.UserIdentifierSortBy.UPDATED_AT,
                sortOrder = SortOrder.ASC,
                userIds = filterUserIds,
                userAuthProviders = filterProviders,
                identifiers = filterStrings
            )
        }
    }
}