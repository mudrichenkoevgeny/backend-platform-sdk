package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserInternal
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
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

class ManagementDeleteUserUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)
    private val authenticationChallengeService = mockk<AuthenticationChallengeService>()

    private val useCase = ManagementDeleteUserUseCase(
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
    fun `successfully deletes user and notifies sessions`() = runTest {
        val permission = UserPermissionCode.USER_DELETE_FOR_USER
        val managerInternal = createTestUserInternal(
            id = managerId,
            role = UserRole.ADMIN,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 100,
            permissionCodes = setOf(permission)
        )
        val targetInternal = createTestUserInternal(
            id = targetId,
            role = UserRole.USER,
            authorityLevel = 50
        )
        val managerSession = mockk<UserSessionInternal>()

        val targetSessionId = UserSessionId.generate()
        val targetSession = mockk<UserSessionInternal> {
            every { id } returns targetSessionId
        }

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { userManager.getUserInternalById(targetId) } returns AppResult.Success(targetInternal)
        coEvery { sessionManager.getUserSessionInternal(sessionId) } returns AppResult.Success(managerSession)
        coEvery {
            authenticationChallengeService.ensureSessionConfirmed(any(), any(), any())
        } returns AppResult.Success(Unit)
        coEvery { sessionManager.getAllUserSessionsInternalList(targetId) } returns AppResult.Success(listOf(targetSession))
        coEvery { userManager.deleteUser(targetId) } returns AppResult.Success(Unit)

        val result = useCase(targetId, context)

        assertEquals(AppResult.Success(Unit), result)
        coVerify { userManager.deleteUser(targetId) }
        coVerify {
            hint(UserSessionId::class)
            webSocketManager.sendMessageToUserSession(targetSessionId, any())
        }
    }

    @Test
    fun `returns error on self deletion attempt`() = runTest {
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(
            status = AuditStatus.FAILED,
            metadata = emptySet()
        )

        val result = useCase(managerId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserForbidden)
    }

    @Test
    fun `returns error when manager authority level is not strictly higher`() = runTest {
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
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(
            status = AuditStatus.FAILED,
            metadata = emptySet()
        )

        val result = useCase(targetId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserInsufficientAuthorityLevel)
    }

    @Test
    fun `returns error when manager lacks specific delete permission for staff`() = runTest {
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
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(
            status = AuditStatus.FAILED,
            metadata = emptySet()
        )

        val result = useCase(targetId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserMissingPermissions)
    }

    @Test
    fun `returns error when target is admin`() = runTest {
        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 100
        )
        val targetInternal = createTestUserInternal(
            id = targetId,
            role = UserRole.ADMIN,
            authorityLevel = 50
        )

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { userManager.getUserInternalById(targetId) } returns AppResult.Success(targetInternal)
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(
            status = AuditStatus.FAILED,
            metadata = emptySet()
        )

        val result = useCase(targetId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserForbidden)
    }
}
