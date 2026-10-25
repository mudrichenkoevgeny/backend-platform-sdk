package io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.user

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserInternal
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.user.toUserPrivate
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetUserUseCaseTest {

    private val userManager = mockk<UserManager>()

    private val useCase = GetUserUseCase(
        userManager = userManager
    )

    private val context = createTestAuthenticatedRequestContext(userRole = UserRole.USER)
    private val userId = context.userId

    @Test
    fun `successfully returns user details`() = runTest {
        val userInternal = createTestUserInternal(id = userId)
        val expectedUserPrivate = userInternal.toUserPrivate()

        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Success(userInternal)

        val result = useCase(context)

        assertEquals(AppResult.Success(expectedUserPrivate), result)
    }

    @Test
    fun `returns forbidden error when user manager returns null`() = runTest {
        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Success(null)

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserForbidden)
    }

    @Test
    fun `returns error when user manager fails`() = runTest {
        val error = UserError.UserNotFound(userId)
        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Error(error)

        val result = useCase(context)

        assertEquals(AppResult.Error(error), result)
    }
}
