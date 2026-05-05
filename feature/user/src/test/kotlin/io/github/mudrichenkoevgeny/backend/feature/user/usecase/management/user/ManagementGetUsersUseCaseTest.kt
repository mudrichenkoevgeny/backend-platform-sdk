package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.mockk.coEvery
import io.mockk.every
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

    private val managerId = UserId.generate()
    private val context = AuthenticatedRequestContext(
        traceId = null,
        userId = managerId,
        userRole = UserRole.ADMIN,
        sessionId = UserSessionId.generate(),
        clientInfo = mockk(relaxed = true)
    )

    private val defaultPageParams = PageParams(page = 1, size = 20)
    private val defaultSortBy = UserSortValues.UserSortBy.CREATED_AT
    private val defaultSortOrder = SortOrder.DESC

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
        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { permissionCodes } returns emptySet()
        }
        val pagedResult = mockk<PagedResult<UserDetails>>()

        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        coEvery {
            userManager.getUsersPageForManagement(
                managementUserPermissionCodes = any(),
                pageParams = any(),
                sortBy = any(),
                sortOrder = any(),
                roles = any(),
                accountStatuses = any(),
                accountStatusesBeforeDeletion = any(),
                authorityLevelFrom = any(),
                authorityLevelTo = any(),
                permissionCodes = any(),
                isTotpEnabled = any()
            )
        } returns AppResult.Success(pagedResult)

        val result = useCase(
            pageParams = defaultPageParams,
            sortBy = defaultSortBy,
            sortOrder = defaultSortOrder,
            roles = emptyList(),
            accountStatuses = emptyList(),
            accountStatusesBeforeDeletion = emptyList(),
            authorityLevelFrom = null,
            authorityLevelTo = null,
            requiredPermissionCodes = emptySet(),
            isTotpEnabled = null,
            authenticatedRequestContext = context
        )

        assertEquals(AppResult.Success(pagedResult), result)
    }

    @Test
    fun `returns error when manager is not active`() = runTest {
        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.BANNED
        }

        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)

        val result = useCase(
            pageParams = defaultPageParams,
            sortBy = defaultSortBy,
            sortOrder = defaultSortOrder,
            roles = emptyList(),
            accountStatuses = emptyList(),
            accountStatusesBeforeDeletion = emptyList(),
            authorityLevelFrom = null,
            authorityLevelTo = null,
            requiredPermissionCodes = emptySet(),
            isTotpEnabled = null,
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserIllegalAccountStatus)
    }

    @Test
    fun `returns error when manager not found`() = runTest {
        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(null)

        val result = useCase(
            pageParams = defaultPageParams,
            sortBy = defaultSortBy,
            sortOrder = defaultSortOrder,
            roles = emptyList(),
            accountStatuses = emptyList(),
            accountStatusesBeforeDeletion = emptyList(),
            authorityLevelFrom = null,
            authorityLevelTo = null,
            requiredPermissionCodes = emptySet(),
            isTotpEnabled = null,
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserForbidden)
    }
}