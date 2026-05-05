package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
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

    private val userId = UserId.generate()
    private val context = AuthenticatedRequestContext(
        traceId = null,
        userId = userId,
        userRole = UserRole.USER,
        sessionId = UserSessionId.generate(),
        clientInfo = ClientInfo()
    )

    @Test
    fun `successfully returns user details`() = runTest {
        val userDetails = mockk<UserDetails>()

        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)

        val result = useCase(context)

        assertEquals(AppResult.Success(userDetails), result)
    }

    @Test
    fun `returns forbidden error when user manager returns null`() = runTest {
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(null)

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserForbidden)
    }

    @Test
    fun `returns error when user manager fails`() = runTest {
        val error = UserError.UserNotFound(userId)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Error(error)

        val result = useCase(context)

        assertEquals(AppResult.Error(error), result)
    }
}