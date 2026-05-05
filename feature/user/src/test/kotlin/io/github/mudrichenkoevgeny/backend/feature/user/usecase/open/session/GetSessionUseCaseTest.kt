package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.session

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
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

class GetSessionUseCaseTest {

    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()

    private val useCase = GetSessionUseCase(
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

    @Test
    fun `successfully returns user session when user is active and session exists`() = runTest {
        val targetSessionId = UserSessionId.generate()
        val userDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
        }
        val userSession = mockk<UserSession>()

        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSelf(targetSessionId) } returns AppResult.Success(userSession)

        val result = useCase(targetSessionId, context)

        assertEquals(AppResult.Success(userSession), result)
    }

    @Test
    fun `returns forbidden error when account status is not allowed`() = runTest {
        val targetSessionId = UserSessionId.generate()
        val userDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.BANNED
        }

        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)

        val result = useCase(targetSessionId, context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserForbidden)
    }

    @Test
    fun `returns not found error when session does not exist`() = runTest {
        val targetSessionId = UserSessionId.generate()
        val userDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
        }

        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSelf(targetSessionId) } returns AppResult.Success(null)

        val result = useCase(targetSessionId, context)

        assertTrue(result is AppResult.Error)
        val error = (result as AppResult.Error).error
        assertTrue(error is CommonError.NotFound)
        assertEquals(targetSessionId.asHexDashString(), (error as CommonError.NotFound).identifier)
    }

    @Test
    fun `returns error when user manager fails`() = runTest {
        val targetSessionId = UserSessionId.generate()
        val error = UserError.UserNotFound(userId)

        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Error(error)

        val result = useCase(targetSessionId, context)

        assertEquals(AppResult.Error(error), result)
    }
}