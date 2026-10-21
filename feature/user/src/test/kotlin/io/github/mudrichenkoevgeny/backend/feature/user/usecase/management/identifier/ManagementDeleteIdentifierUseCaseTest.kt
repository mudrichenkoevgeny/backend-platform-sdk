package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.error.CommonAuditErrorParser
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.identifier.createTestUserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.session.createTestUserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.testing.feature.user.domain.model.user.createTestUserInternal
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.IdentifierPermissionCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManagementDeleteIdentifierUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val commonAuditErrorParser = CommonAuditErrorParser()
    private val auditErrorConverter = AuditErrorConverter(emptySet(), commonAuditErrorParser)
    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()
    private val identifierManager = mockk<IdentifierManager>()
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)
    private val authenticationChallengeService = mockk<AuthenticationChallengeService>()

    private val useCase = ManagementDeleteIdentifierUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        userManager = userManager,
        sessionManager = sessionManager,
        identifierManager = identifierManager,
        webSocketManager = webSocketManager,
        authenticationChallengeService = authenticationChallengeService
    )

    @Test
    fun `successfully deletes identifier and notifies associated sessions`() = runTest {
        val managerId = UserId.generate()
        val targetUserId = UserId.generate()
        val targetIdentifierId = UserIdentifierId.generate()
        val targetSessionId = UserSessionId.generate()
        val context = createTestAuthenticatedRequestContext(userId = managerId)

        val managerInternal = createTestUserInternal(
            id = managerId,
            role = UserRole.ADMIN,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 100,
            permissionCodes = setOf(PermissionCode(IdentifierPermissionCode.IDENTIFIER_DELETE_FOR_USER.value))
        )

        val targetIdentifier = createTestUserIdentifierInternal(id = targetIdentifierId, userId = targetUserId)

        val targetUserInternal = createTestUserInternal(
            id = targetUserId,
            role = UserRole.USER,
            authorityLevel = 50,
            accountStatus = UserAccountStatus.ACTIVE
        )

        val identifiersList = listOf(targetIdentifier, createTestUserIdentifierInternal(userId = targetUserId))
        val managerSession = createTestUserSessionInternal(id = context.sessionId, userId = managerId)
        val targetSession = createTestUserSessionInternal(id = targetSessionId, userId = targetUserId)

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { identifierManager.getUserIdentifierInternalById(targetIdentifierId) } returns AppResult.Success(targetIdentifier)
        coEvery { userManager.getUserInternalById(targetUserId) } returns AppResult.Success(targetUserInternal)
        coEvery { identifierManager.getUserIdentifiersInternalPageByUserId(targetUserId) } returns AppResult.Success(identifiersList)
        coEvery { sessionManager.getUserSessionInternal(context.sessionId) } returns AppResult.Success(managerSession)
        coEvery { authenticationChallengeService.ensureSessionConfirmed(any(), any(), any()) } returns AppResult.Success(Unit)
        coEvery { sessionManager.getUserSessionsInternalListByIdentifierId(targetIdentifierId, targetUserId) } returns AppResult.Success(listOf(targetSession))
        coEvery { identifierManager.deleteUserIdentifier(targetIdentifierId) } returns AppResult.Success(Unit)

        val result = useCase(targetIdentifierId, context)

        assertEquals(AppResult.Success(Unit), result)

        coVerify { webSocketManager.sendMessageToUserSession(targetSessionId, any()) }
        coVerify {
            auditLogger.log(
                actorId = managerId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.value,
                action = UserAuditActionType.MANAGEMENT_DELETE_IDENTIFIER,
                resource = UserAuditResourceType.IDENTIFIER,
                resourceId = targetIdentifierId.asHexDashString(),
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when attempting to delete self identifier`() = runTest {
        val managerId = UserId.generate()
        val targetIdentifierId = UserIdentifierId.generate()
        val context = createTestAuthenticatedRequestContext(userId = managerId)

        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 100
        )

        val targetIdentifier = createTestUserIdentifierInternal(id = targetIdentifierId, userId = managerId)

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { identifierManager.getUserIdentifierInternalById(targetIdentifierId) } returns AppResult.Success(targetIdentifier)

        val result = useCase(targetIdentifierId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserForbidden)
    }

    @Test
    fun `returns error when target user has higher authority level`() = runTest {
        val managerId = UserId.generate()
        val targetUserId = UserId.generate()
        val targetIdentifierId = UserIdentifierId.generate()
        val context = createTestAuthenticatedRequestContext(userId = managerId)

        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 50
        )

        val targetIdentifier = createTestUserIdentifierInternal(id = targetIdentifierId, userId = targetUserId)

        val targetUserInternal = createTestUserInternal(
            id = targetUserId,
            authorityLevel = 100,
            accountStatus = UserAccountStatus.ACTIVE
        )

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { identifierManager.getUserIdentifierInternalById(targetIdentifierId) } returns AppResult.Success(targetIdentifier)
        coEvery { userManager.getUserInternalById(targetUserId) } returns AppResult.Success(targetUserInternal)

        val result = useCase(targetIdentifierId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserInsufficientAuthorityLevel)
    }

    @Test
    fun `returns error when target user has only one identifier remaining`() = runTest {
        val managerId = UserId.generate()
        val targetUserId = UserId.generate()
        val targetIdentifierId = UserIdentifierId.generate()
        val context = createTestAuthenticatedRequestContext(userId = managerId)

        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 100,
            role = UserRole.ADMIN,
            permissionCodes = setOf(PermissionCode(IdentifierPermissionCode.IDENTIFIER_DELETE_FOR_USER.value))
        )

        val targetIdentifier = createTestUserIdentifierInternal(id = targetIdentifierId, userId = targetUserId)

        val targetUserInternal = createTestUserInternal(
            id = targetUserId,
            role = UserRole.USER,
            authorityLevel = 50,
            accountStatus = UserAccountStatus.ACTIVE
        )

        val identifiersList = listOf(targetIdentifier)

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { identifierManager.getUserIdentifierInternalById(targetIdentifierId) } returns AppResult.Success(targetIdentifier)
        coEvery { userManager.getUserInternalById(targetUserId) } returns AppResult.Success(targetUserInternal)
        coEvery { identifierManager.getUserIdentifiersInternalPageByUserId(targetUserId) } returns AppResult.Success(identifiersList)

        val result = useCase(targetIdentifierId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.CannotDeleteUserIdentifier)
    }

    @Test
    fun `returns error when rate limit is exceeded`() = runTest {
        val managerId = UserId.generate()
        val targetIdentifierId = UserIdentifierId.generate()
        val context = createTestAuthenticatedRequestContext(userId = managerId)
        val limitError = CommonError.Unknown()

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Error(limitError)

        val result = useCase(targetIdentifierId, context)

        assertTrue(result is AppResult.Error && result.error == limitError)
        coVerify(exactly = 0) { identifierManager.deleteUserIdentifier(any()) }
    }
}
