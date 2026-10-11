package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.identifier.createTestUserIdentifierPrivate
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.createTestUserInternal
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
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordhash.PasswordHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
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

class ManagementDeleteIdentifierPasswordUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()
    private val identifierManager = mockk<IdentifierManager>()
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)
    private val authenticationChallengeService = mockk<AuthenticationChallengeService>()

    private val useCase = ManagementDeleteIdentifierPasswordUseCase(
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
    fun `successfully revokes identifier password and terminates associated sessions`() = runTest {
        val managerId = UserId.generate()
        val targetUserId = UserId.generate()
        val targetIdentifierId = UserIdentifierId.generate()
        val targetSessionId = UserSessionId.generate()
        val context = createTestAuthenticatedRequestContext(userId = managerId)

        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 100,
            role = UserRole.ADMIN,
            permissionCodes = setOf(
                PermissionCode(IdentifierPermissionCode.IDENTIFIER_DELETE_PASSWORD_FOR_USER.value)
            )
        )

        val targetIdentifier = mockk<UserIdentifierInternal> {
            every { userId } returns targetUserId
            every { userAuthProvider } returns UserAuthProvider.EMAIL
            every { passwordHash } returns PasswordHash("hashed_password")
        }

        val targetUserInternal = createTestUserInternal(
            id = targetUserId,
            role = UserRole.USER,
            authorityLevel = 50,
            accountStatus = UserAccountStatus.ACTIVE
        )

        val managerSession = mockk<UserSessionInternal>()
        val targetSession = mockk<UserSessionInternal> {
            every { id } returns targetSessionId
        }
        val updatedUserIdentifier = createTestUserIdentifierPrivate()

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { identifierManager.getUserIdentifierInternalById(targetIdentifierId) } returns AppResult.Success(targetIdentifier)
        coEvery { userManager.getUserInternalById(targetUserId) } returns AppResult.Success(targetUserInternal)
        coEvery { sessionManager.getUserSessionInternal(context.sessionId) } returns AppResult.Success(managerSession)
        coEvery { authenticationChallengeService.ensureSessionConfirmed(any(), any(), any()) } returns AppResult.Success(Unit)
        coEvery { sessionManager.getUserSessionsInternalListByIdentifierId(targetIdentifierId, targetUserId) } returns AppResult.Success(listOf(targetSession))
        coEvery { identifierManager.clearUserIdentifierPassword(targetIdentifier) } returns AppResult.Success(updatedUserIdentifier)
        coEvery { sessionManager.deleteSessionById(targetSessionId) } returns AppResult.Success(Unit)

        val result = useCase(targetIdentifierId, context)

        assertEquals(AppResult.Success(Unit), result)

        coVerify { sessionManager.deleteSessionById(targetSessionId) }
        coVerify { webSocketManager.sendMessageToUserSession(targetSessionId, any()) }
        coVerify {
            auditLogger.log(
                actorId = managerId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.MANAGEMENT_DELETE_IDENTIFIER_PASSWORD,
                resource = UserAuditResourceType.IDENTIFIER,
                resourceId = targetIdentifierId.asHexDashString(),
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error UserIdentifierPasswordNotSupported when provider is not EMAIL`() = runTest {
        val managerId = UserId.generate()
        val targetUserId = UserId.generate()
        val targetIdentifierId = UserIdentifierId.generate()
        val context = createTestAuthenticatedRequestContext(userId = managerId)

        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 100,
            role = UserRole.ADMIN,
            permissionCodes = setOf(
                PermissionCode(IdentifierPermissionCode.IDENTIFIER_DELETE_PASSWORD_FOR_USER.value)
            )
        )

        val targetIdentifier = mockk<UserIdentifierInternal> {
            every { userId } returns targetUserId
            every { userAuthProvider } returns UserAuthProvider.GOOGLE
            every { passwordHash } returns null
        }

        val targetUserInternal = createTestUserInternal(
            id = targetUserId,
            role = UserRole.USER,
            authorityLevel = 50,
            accountStatus = UserAccountStatus.ACTIVE
        )

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { identifierManager.getUserIdentifierInternalById(targetIdentifierId) } returns AppResult.Success(targetIdentifier)
        coEvery { userManager.getUserInternalById(targetUserId) } returns AppResult.Success(targetUserInternal)
        every { auditErrorConverter.convert(any<UserError.UserIdentifierPasswordNotSupported>()) } returns AuditErrorLogData(
            status = AuditStatus.FAILED,
            metadata = emptySet()
        )

        val result = useCase(targetIdentifierId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserIdentifierPasswordNotSupported)
    }

    @Test
    fun `returns error UserIdentifierPasswordNotSet when identifier has no password set`() = runTest {
        val managerId = UserId.generate()
        val targetUserId = UserId.generate()
        val targetIdentifierId = UserIdentifierId.generate()
        val context = createTestAuthenticatedRequestContext(userId = managerId)

        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 100,
            role = UserRole.ADMIN,
            permissionCodes = setOf(
                PermissionCode(IdentifierPermissionCode.IDENTIFIER_DELETE_PASSWORD_FOR_USER.value)
            )
        )

        val targetIdentifier = mockk<UserIdentifierInternal> {
            every { userId } returns targetUserId
            every { userAuthProvider } returns UserAuthProvider.EMAIL
            every { passwordHash } returns null
        }

        val targetUserInternal = createTestUserInternal(
            id = targetUserId,
            role = UserRole.USER,
            authorityLevel = 50,
            accountStatus = UserAccountStatus.ACTIVE
        )

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { identifierManager.getUserIdentifierInternalById(targetIdentifierId) } returns AppResult.Success(targetIdentifier)
        coEvery { userManager.getUserInternalById(targetUserId) } returns AppResult.Success(targetUserInternal)
        every { auditErrorConverter.convert(any<UserError.UserIdentifierPasswordNotSet>()) } returns AuditErrorLogData(
            status = AuditStatus.FAILED,
            metadata = emptySet()
        )

        val result = useCase(targetIdentifierId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserIdentifierPasswordNotSet)
    }

    @Test
    fun `returns error when attempting to delete password of self identifier`() = runTest {
        val managerId = UserId.generate()
        val targetIdentifierId = UserIdentifierId.generate()
        val context = createTestAuthenticatedRequestContext(userId = managerId)

        val managerInternal = createTestUserInternal(
            id = managerId,
            accountStatus = UserAccountStatus.ACTIVE,
            authorityLevel = 100
        )

        val targetIdentifier = mockk<UserIdentifierInternal> {
            every { userId } returns managerId
        }

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { identifierManager.getUserIdentifierInternalById(targetIdentifierId) } returns AppResult.Success(targetIdentifier)
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())

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

        val targetIdentifier = mockk<UserIdentifierInternal> {
            every { userId } returns targetUserId
        }

        val targetUserInternal = createTestUserInternal(
            id = targetUserId,
            authorityLevel = 100,
            accountStatus = UserAccountStatus.ACTIVE
        )

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(managerId) } returns AppResult.Success(managerInternal)
        coEvery { identifierManager.getUserIdentifierInternalById(targetIdentifierId) } returns AppResult.Success(targetIdentifier)
        coEvery { userManager.getUserInternalById(targetUserId) } returns AppResult.Success(targetUserInternal)
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())

        val result = useCase(targetIdentifierId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserInsufficientAuthorityLevel)
    }

    @Test
    fun `returns error when rate limit is exceeded`() = runTest {
        val managerId = UserId.generate()
        val targetIdentifierId = UserIdentifierId.generate()
        val context = createTestAuthenticatedRequestContext(userId = managerId)
        val limitError = mockk<AppError>()

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Error(limitError)
        every { auditErrorConverter.convert(limitError) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())

        val result = useCase(targetIdentifierId, context)

        assertTrue(result is AppResult.Error && result.error == limitError)
        coVerify(exactly = 0) { identifierManager.clearUserIdentifierPassword(any()) }
    }
}
