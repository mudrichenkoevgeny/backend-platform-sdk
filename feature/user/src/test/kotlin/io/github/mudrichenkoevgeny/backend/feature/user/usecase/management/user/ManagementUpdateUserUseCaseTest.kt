package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.UserPermissionCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManagementUpdateUserUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)
    private val authenticationChallengeService = mockk<AuthenticationChallengeService>()

    private val useCase = ManagementUpdateUserUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        userManager = userManager,
        sessionManager = sessionManager,
        webSocketManager = webSocketManager,
        authenticationChallengeService = authenticationChallengeService
    )

    private val managerId = UserId.generate()
    private val targetId = UserId.generate()
    private val sessionId = UserSessionId.generate()
    private val context = AuthenticatedRequestContext(
        traceId = null,
        userId = managerId,
        userRole = UserRole.ADMIN,
        sessionId = sessionId,
        clientInfo = mockk(relaxed = true)
    )

    @Test
    fun `successfully updates user permissions and notifies sessions`() = runTest {
        val permission = UserPermissionCode.USER_UPDATE_PERMISSIONS_FOR_USER
        val newPermission = UserPermissionCode.USER_CREATE_AS_USER
        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { authorityLevel } returns 100
            every { permissionCodes } returns setOf(permission, newPermission)
        }
        val targetDetails = mockk<UserDetails> {
            every { id } returns targetId
            every { authorityLevel } returns 50
            every { role } returns UserRole.USER
        }
        val updatedUser = mockk<UserDetails>()
        val managerSession = mockk<UserSessionInternal>()

        val targetSessionId = UserSessionId.generate()
        val targetSession = mockk<UserSessionInternal> {
            every { id } returns targetSessionId
        }

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        coEvery { userManager.getUserByIdForSelf(targetId) } returns AppResult.Success(targetDetails)
        coEvery { sessionManager.getUserSessionForSystem(sessionId) } returns AppResult.Success(managerSession)
        coEvery {
            authenticationChallengeService.ensureSessionConfirmed(any(), any())
        } returns AppResult.Success(Unit)
        coEvery { sessionManager.getAllUserSessions(targetId) } returns AppResult.Success(listOf(targetSession))
        coEvery {
            userManager.updateUserForManagement(targetDetails, null, null, setOf(newPermission))
        } returns AppResult.Success(updatedUser)

        val result = useCase(
            userId = targetId,
            permissionCodes = setOf(newPermission),
            authenticatedRequestContext = context
        )

        assertEquals(AppResult.Success(updatedUser), result)

        coVerify {
            hint(UserSessionId::class)
            webSocketManager.sendMessageToUserSession(
                userSessionId = targetSessionId,
                frame = any()
            )
        }
    }

    @Test
    fun `returns error when manager authority level is not higher than target`() = runTest {
        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { authorityLevel } returns 50
        }
        val targetDetails = mockk<UserDetails> {
            every { authorityLevel } returns 50
        }

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        coEvery { userManager.getUserByIdForSelf(targetId) } returns AppResult.Success(targetDetails)
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())

        val result = useCase(targetId, authorityLevel = 40, authenticatedRequestContext = context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserInsufficientAuthorityLevel)
    }

    @Test
    fun `returns error when manager grants permission they do not have`() = runTest {
        val permission = UserPermissionCode.USER_UPDATE_PERMISSIONS_FOR_USER
        val forbiddenPermission = UserPermissionCode.USER_UPDATE_SECURITY_FOR_STAFF
        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { authorityLevel } returns 100
            every { permissionCodes } returns setOf(permission)
        }
        val targetDetails = mockk<UserDetails> {
            every { authorityLevel } returns 50
            every { role } returns UserRole.USER
        }

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        coEvery { userManager.getUserByIdForSelf(targetId) } returns AppResult.Success(targetDetails)
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())

        val result = useCase(
            userId = targetId,
            permissionCodes = setOf(forbiddenPermission),
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserMissingPermissions)
    }

    @Test
    fun `returns error when manager lacks specific status update permission for staff`() = runTest {
        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { authorityLevel } returns 100
            every { permissionCodes } returns emptySet()
        }
        val targetDetails = mockk<UserDetails> {
            every { authorityLevel } returns 50
            every { role } returns UserRole.STAFF
        }

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        coEvery { userManager.getUserByIdForSelf(targetId) } returns AppResult.Success(targetDetails)
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())

        val result = useCase(
            userId = targetId,
            accountStatus = UserAccountStatus.BANNED,
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserMissingPermissions)
    }

    @Test
    fun `returns error when new authority level is equal to manager's`() = runTest {
        val managerDetails = mockk<UserDetails> {
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { authorityLevel } returns 80
        }
        val targetDetails = mockk<UserDetails> {
            every { authorityLevel } returns 50
        }

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        coEvery { userManager.getUserByIdForSelf(targetId) } returns AppResult.Success(targetDetails)
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())

        val result = useCase(
            userId = targetId,
            authorityLevel = 80,
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserInsufficientAuthorityLevel)
    }
}