package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.session

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
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

class ManagementGetSessionUseCaseTest {

    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()

    private val useCase = ManagementGetSessionUseCase(
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
    fun `successfully retrieves user session for management`() = runTest {
        val targetSessionId = UserSessionId.generate()
        val managementUserId = defaultContext.userId
        val expectedSession = mockk<UserSession>()

        val managementUser = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { permissionCodes } returns emptySet()
        }

        coEvery { userManager.getUserByIdForSelf(managementUserId) } returns AppResult.Success(managementUser)
        coEvery {
            sessionManager.getUserSessionForManagement(
                userSessionId = targetSessionId,
                managementUserId = managementUserId,
                managementUserPermissionCodes = any()
            )
        } returns AppResult.Success(expectedSession)

        val result = useCase(targetSessionId, defaultContext)

        assertEquals(AppResult.Success(expectedSession), result)
    }

    @Test
    fun `returns NotFound when session does not exist or access denied by manager`() = runTest {
        val targetSessionId = UserSessionId.generate()
        val managementUserId = defaultContext.userId

        val managementUser = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { permissionCodes } returns emptySet()
        }

        coEvery { userManager.getUserByIdForSelf(managementUserId) } returns AppResult.Success(managementUser)
        coEvery {
            sessionManager.getUserSessionForManagement(any(), any(), any())
        } returns AppResult.Success(null)

        val result = useCase(targetSessionId, defaultContext)

        assertTrue(result is AppResult.Error && result.error is CommonError.NotFound)
    }

    @Test
    fun `returns error when management user is not active`() = runTest {
        val targetSessionId = UserSessionId.generate()
        val managementUserId = defaultContext.userId
        val managementUser = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.BANNED
        }

        coEvery { userManager.getUserByIdForSelf(managementUserId) } returns AppResult.Success(managementUser)

        val result = useCase(targetSessionId, defaultContext)

        assertTrue(result is AppResult.Error && result.error is UserError.UserIllegalAccountStatus)
    }

    @Test
    fun `returns error when management user not found`() = runTest {
        val targetSessionId = UserSessionId.generate()
        coEvery { userManager.getUserByIdForSelf(any()) } returns AppResult.Success(null)

        val result = useCase(targetSessionId, defaultContext)

        assertTrue(result is AppResult.Error && result.error is UserError.UserForbidden)
    }
}