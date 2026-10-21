package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserInternal
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.user.toUserPrivate
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

class ManagementGetUserUseCaseTest {

    private val userManager = mockk<UserManager>()
    private val useCase = ManagementGetUserUseCase(userManager)

    private val context = createTestAuthenticatedRequestContext()
    private val managerId = context.userId
    private val targetId = UserId.generate()

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
        val managerInternal = createTestUserInternal(id = managerId, accountStatus = UserAccountStatus.ACTIVE)
        val targetInternal = createTestUserInternal(id = targetId)
        val expectedTargetPrivate = targetInternal.toUserPrivate()

        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery {
            userManager.getUserInternal(
                userId = targetId,
                managementUserId = managerId,
                managementUserPermissionCodes = any()
            )
        } returns AppResult.Success(targetInternal)

        val result = useCase(targetId, context)

        assertEquals(AppResult.Success(expectedTargetPrivate), result)
    }

    @Test
    fun `returns error when manager is not active`() = runTest {
        val managerInternal = createTestUserInternal(id = managerId, accountStatus = UserAccountStatus.BANNED)

        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)

        val result = useCase(targetId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserIllegalAccountStatus)
    }

    @Test
    fun `returns error when manager is not found`() = runTest {
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(null)

        val result = useCase(targetId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserForbidden)
    }

    @Test
    fun `returns error when target user is not found or not authorized`() = runTest {
        val managerInternal = createTestUserInternal(id = managerId, accountStatus = UserAccountStatus.ACTIVE)

        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery {
            userManager.getUserInternal(
                userId = targetId,
                managementUserId = managerId,
                managementUserPermissionCodes = any()
            )
        } returns AppResult.Success(null)

        val result = useCase(targetId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserNotFound)
    }
}
