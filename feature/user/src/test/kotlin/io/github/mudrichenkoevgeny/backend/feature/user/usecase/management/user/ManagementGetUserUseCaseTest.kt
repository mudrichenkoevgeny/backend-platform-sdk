package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
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

class ManagementGetUserUseCaseTest {

    private val userManager = mockk<UserManager>()
    private val useCase = ManagementGetUserUseCase(userManager)

    private val managerId = UserId.generate()
    private val targetId = UserId.generate()
    private val context = AuthenticatedRequestContext(
        traceId = null,
        userId = managerId,
        userRole = UserRole.ADMIN,
        sessionId = UserSessionId.generate(),
        clientInfo = mockk(relaxed = true)
    )

    @BeforeEach
    fun setUp() {
        mockkStatic(UserId::class)
        mockkStatic(UserSessionId::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UserId::class)
        unmockkStatic(UserSessionId::class)
    }

    @Test
    fun `successfully returns user details when manager is active and authorized`() = runTest {
        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { permissionCodes } returns emptySet()
        }
        val targetDetails = mockk<UserDetails>()

        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        coEvery {
            userManager.getUserForManagement(
                userId = targetId,
                managementUserId = managerId,
                managementUserPermissionCodes = any()
            )
        } returns AppResult.Success(targetDetails)

        val result = useCase(targetId, context)

        assertEquals(AppResult.Success(targetDetails), result)
    }

    @Test
    fun `returns error when manager is not active`() = runTest {
        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.BANNED
        }

        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)

        val result = useCase(targetId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserIllegalAccountStatus)
    }

    @Test
    fun `returns error when manager is not found`() = runTest {
        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(null)

        val result = useCase(targetId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserForbidden)
    }

    @Test
    fun `returns error when target user is not found or not authorized`() = runTest {
        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { permissionCodes } returns emptySet()
        }

        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        coEvery {
            userManager.getUserForManagement(
                userId = targetId,
                managementUserId = managerId,
                managementUserPermissionCodes = any()
            )
        } returns AppResult.Success(null)

        val result = useCase(targetId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserNotFound)
    }
}