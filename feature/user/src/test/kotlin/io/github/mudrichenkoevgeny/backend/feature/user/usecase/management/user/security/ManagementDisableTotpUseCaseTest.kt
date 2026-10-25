package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user.security

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.error.CommonAuditErrorParser
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.session.createTestUserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserInternal
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.totp.TotpManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.UserPermissionCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManagementDisableTotpUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val commonAuditErrorParser = CommonAuditErrorParser()
    private val auditErrorConverter = AuditErrorConverter(emptySet(), commonAuditErrorParser)
    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()
    private val totpManager = mockk<TotpManager>()
    private val authenticationChallengeService = mockk<AuthenticationChallengeService>()

    private val useCase = ManagementDisableTotpUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        userManager = userManager,
        sessionManager = sessionManager,
        totpManager = totpManager,
        authenticationChallengeService = authenticationChallengeService
    )

    private val context = createTestAuthenticatedRequestContext()
    private val managerId = context.userId
    private val targetId = UserId.generate()
    private val sessionId = context.sessionId

    @Test
    fun `successfully disables TOTP for user`() = runTest {
        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 100,
            permissionCodes = setOf(UserPermissionCode.USER_UPDATE_SECURITY_FOR_USER)
        )

        val targetInternal = createTestUserInternal(
            id = targetId,
            isTotpEnabled = true,
            authorityLevel = 50,
            role = UserRole.USER
        )

        val managerSession = createTestUserSessionInternal(id = sessionId, userId = managerId)

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { userManager.getUserInternalById(targetId) } returns AppResult.Success(targetInternal)
        coEvery { sessionManager.getUserSessionInternal(sessionId) } returns AppResult.Success(managerSession)
        coEvery { authenticationChallengeService.ensureSessionConfirmed(any(), any(), any()) } returns AppResult.Success(Unit)
        coEvery { totpManager.disableTotp(targetId) } returns AppResult.Success(Unit)

        val result = useCase(targetId, context)

        assertEquals(AppResult.Success(Unit), result)
        coVerify { totpManager.disableTotp(targetId) }
        coVerify {
            auditLogger.log(
                actorId = managerId.asHexDashString(),
                actorType = AuditActorType.USER,
                action = UserAuditActionType.MANAGEMENT_DISABLE_USER_TOTP,
                resource = UserAuditResourceType.USER,
                resourceId = targetId.asHexDashString(),
                status = AuditStatus.SUCCESS,
                metadata = any(),
                actorUserRole = any()
            )
        }
    }

    @Test
    fun `returns error when attempting to disable own TOTP via management`() = runTest {
        val result = useCase(managerId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserForbidden)
    }

    @Test
    fun `returns error when target user does not have TOTP enabled`() = runTest {
        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.ACTIVE
        )
        val targetInternal = createTestUserInternal(
            id = targetId,
            isTotpEnabled = false
        )

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { userManager.getUserInternalById(targetId) } returns AppResult.Success(targetInternal)

        val result = useCase(targetId, context)

        assertTrue(result is AppResult.Error && result.error is SecurityError.TotpNotEnabled)
    }

    @Test
    fun `returns error when manager has lower or equal authority level`() = runTest {
        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 50
        )
        val targetInternal = createTestUserInternal(
            id = targetId,
            isTotpEnabled = true,
            authorityLevel = 50
        )

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { userManager.getUserInternalById(targetId) } returns AppResult.Success(targetInternal)

        val result = useCase(targetId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserInsufficientAuthorityLevel)
    }

    @Test
    fun `returns error when manager lacks specific permissions for staff target`() = runTest {
        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 100,
            permissionCodes = emptySet()
        )
        val targetInternal = createTestUserInternal(
            id = targetId,
            isTotpEnabled = true,
            authorityLevel = 50,
            role = UserRole.STAFF
        )

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { userManager.getUserInternalById(targetId) } returns AppResult.Success(targetInternal)

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
            isTotpEnabled = true,
            authorityLevel = 50,
            role = UserRole.ADMIN
        )

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { userManager.getUserInternalById(targetId) } returns AppResult.Success(targetInternal)

        val result = useCase(targetId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserForbidden)
    }
}
