package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.error.CommonAuditErrorParser
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.session.createTestUserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserInternal
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserPrivate
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.UserPermissionCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManagementUpdateUserUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val commonAuditErrorParser = CommonAuditErrorParser()
    private val auditErrorConverter = AuditErrorConverter(emptySet(), commonAuditErrorParser)
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

    private val context = createTestAuthenticatedRequestContext()
    private val managerId = context.userId
    private val targetId = UserId.generate()
    private val sessionId = context.sessionId

    @Test
    fun `successfully updates user permissions and notifies sessions`() = runTest {
        val permission = UserPermissionCode.USER_UPDATE_PERMISSIONS_FOR_USER
        val newPermission = UserPermissionCode.USER_CREATE_AS_USER
        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 100,
            permissionCodes = setOf(permission, newPermission)
        )
        val targetInternal = createTestUserInternal(
            id = targetId,
            role = UserRole.USER,
            authorityLevel = 50
        )
        val updatedUserInternal = createTestUserInternal(id = targetId, permissionCodes = setOf(newPermission))
        val expectedUserPrivate = createTestUserPrivate(id = targetId, permissionCodes = setOf(newPermission))
        val managerSession = createTestUserSessionInternal(id = sessionId, userId = managerId)

        val targetSessionId = UserSessionId.generate()
        val targetSession = createTestUserSessionInternal(id = targetSessionId, userId = targetId)

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { userManager.getUserInternalById(targetId) } returns AppResult.Success(targetInternal)
        coEvery { sessionManager.getUserSessionInternal(sessionId) } returns AppResult.Success(managerSession)
        coEvery {
            authenticationChallengeService.ensureSessionConfirmed(any(), any(), any())
        } returns AppResult.Success(Unit)
        coEvery { sessionManager.getAllUserSessionsInternalList(targetId) } returns AppResult.Success(listOf(targetSession))
        coEvery {
            userManager.updateUser(any(), any(), any(), any(), any(), any(), any())
        } returns AppResult.Success(updatedUserInternal)

        val result = useCase(
            userId = targetId,
            permissionCodes = setOf(newPermission),
            authenticatedRequestContext = context
        )

        assertEquals(AppResult.Success(expectedUserPrivate), result)

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
        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 50
        )
        val targetInternal = createTestUserInternal(
            id = targetId,
            authorityLevel = 50
        )

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { userManager.getUserInternalById(targetId) } returns AppResult.Success(targetInternal)

        val result = useCase(targetId, authorityLevel = 40, authenticatedRequestContext = context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserInsufficientAuthorityLevel)
    }

    @Test
    fun `returns error when manager grants permission they do not have`() = runTest {
        val permission = UserPermissionCode.USER_UPDATE_PERMISSIONS_FOR_USER
        val forbiddenPermission = UserPermissionCode.USER_UPDATE_SECURITY_FOR_STAFF
        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 100,
            permissionCodes = setOf(permission)
        )
        val targetInternal = createTestUserInternal(
            id = targetId,
            role = UserRole.USER,
            authorityLevel = 50
        )

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { userManager.getUserInternalById(targetId) } returns AppResult.Success(targetInternal)

        val result = useCase(
            userId = targetId,
            permissionCodes = setOf(forbiddenPermission),
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserMissingPermissions)
    }

    @Test
    fun `returns error when manager lacks specific status update permission for staff`() = runTest {
        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 100,
            permissionCodes = emptySet()
        )
        val targetInternal = createTestUserInternal(
            id = targetId,
            role = UserRole.STAFF,
            authorityLevel = 50
        )

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { userManager.getUserInternalById(targetId) } returns AppResult.Success(targetInternal)

        val result = useCase(
            userId = targetId,
            accountStatus = UserAccountStatus.BANNED,
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserMissingPermissions)
    }

    @Test
    fun `returns error when new authority level is equal to manager's`() = runTest {
        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 80
        )
        val targetInternal = createTestUserInternal(
            id = targetId,
            authorityLevel = 50
        )

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { userManager.getUserInternalById(targetId) } returns AppResult.Success(targetInternal)

        val result = useCase(
            userId = targetId,
            authorityLevel = 80,
            authenticatedRequestContext = context
        )

        assertTrue(result is AppResult.Error && result.error is UserError.UserInsufficientAuthorityLevel)
    }
}
