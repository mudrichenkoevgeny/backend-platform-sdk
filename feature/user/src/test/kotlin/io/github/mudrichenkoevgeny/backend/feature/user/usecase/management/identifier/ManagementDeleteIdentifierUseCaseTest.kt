package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
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
    private val auditErrorConverter = mockk<AuditErrorConverter>()
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

    private fun createAuthContext(managerId: UserId) = AuthenticatedRequestContext(
        traceId = null,
        userId = managerId,
        userRole = UserRole.ADMIN,
        sessionId = UserSessionId.generate(),
        clientInfo = ClientInfo()
    )

    @Test
    fun `successfully deletes identifier and notifies associated sessions`() = runTest {
        val managerId = UserId.generate()
        val targetUserId = UserId.generate()
        val targetIdentifierId = UserIdentifierId.generate()
        val targetSessionId = UserSessionId.generate()
        val context = createAuthContext(managerId)

        val managerDetails = mockk<UserDetails> {
            every { id } returns managerId
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { authorityLevel } returns 100
            every { role } returns UserRole.ADMIN
            every { permissionCodes } returns setOf(PermissionCode(IdentifierPermissionCode.IDENTIFIER_DELETE_FOR_USER.value))
        }

        val targetIdentifier = mockk<UserIdentifierInternal> {
            every { userId } returns targetUserId
        }

        val targetUserDetails = mockk<UserDetails> {
            every { id } returns targetUserId
            every { role } returns UserRole.USER
            every { authorityLevel } returns 50
            every { accountStatus } returns UserAccountStatus.ACTIVE
        }

        val identifiersList = listOf(targetIdentifier, mockk<UserIdentifierInternal>())
        val managerSession = mockk<UserSessionInternal>()
        val targetSession = mockk<UserSessionInternal> {
            every { id } returns targetSessionId
        }

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        coEvery { identifierManager.getUserIdentifierByIdForSystem(targetIdentifierId) } returns AppResult.Success(targetIdentifier)
        coEvery { userManager.getUserByIdForSelf(targetUserId) } returns AppResult.Success(targetUserDetails)
        coEvery { identifierManager.getUserIdentifiersByUserId(targetUserId) } returns AppResult.Success(identifiersList)
        coEvery { sessionManager.getUserSessionForSystem(context.sessionId) } returns AppResult.Success(managerSession)
        coEvery { authenticationChallengeService.ensureSessionConfirmed(any(), any()) } returns AppResult.Success(Unit)
        coEvery { sessionManager.getUserSessionsByIdentifierId(targetIdentifierId, targetUserId) } returns AppResult.Success(listOf(targetSession))
        coEvery { identifierManager.deleteUserIdentifier(targetIdentifierId) } returns AppResult.Success(Unit)

        val result = useCase(targetIdentifierId, context)

        assertEquals(AppResult.Success(Unit), result)

        coVerify { webSocketManager.sendMessageToUserSession(targetSessionId, any()) }
        coVerify {
            auditLogger.log(
                actorId = managerId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
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
        val context = createAuthContext(managerId)

        val managerDetails = mockk<UserDetails> {
            every { id } returns managerId
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { authorityLevel } returns 100
        }

        val targetIdentifier = mockk<UserIdentifierInternal> {
            every { userId } returns managerId
        }

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        coEvery { identifierManager.getUserIdentifierByIdForSystem(targetIdentifierId) } returns AppResult.Success(targetIdentifier)
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())

        val result = useCase(targetIdentifierId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserForbidden)
    }

    @Test
    fun `returns error when target user has higher authority level`() = runTest {
        val managerId = UserId.generate()
        val targetUserId = UserId.generate()
        val targetIdentifierId = UserIdentifierId.generate()
        val context = createAuthContext(managerId)

        val managerDetails = mockk<UserDetails> {
            every { id } returns managerId
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { authorityLevel } returns 50
        }

        val targetIdentifier = mockk<UserIdentifierInternal> {
            every { userId } returns targetUserId
        }

        val targetUserDetails = mockk<UserDetails> {
            every { id } returns targetUserId
            every { authorityLevel } returns 100
            every { accountStatus } returns UserAccountStatus.ACTIVE
        }

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        coEvery { identifierManager.getUserIdentifierByIdForSystem(targetIdentifierId) } returns AppResult.Success(targetIdentifier)
        coEvery { userManager.getUserByIdForSelf(targetUserId) } returns AppResult.Success(targetUserDetails)
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())

        val result = useCase(targetIdentifierId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.UserInsufficientAuthorityLevel)
    }

    @Test
    fun `returns error when target user has only one identifier remaining`() = runTest {
        val managerId = UserId.generate()
        val targetUserId = UserId.generate()
        val targetIdentifierId = UserIdentifierId.generate()
        val context = createAuthContext(managerId)

        val managerDetails = mockk<UserDetails> {
            every { id } returns managerId
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { authorityLevel } returns 100
            every { role } returns UserRole.ADMIN
            every { permissionCodes } returns setOf(PermissionCode(IdentifierPermissionCode.IDENTIFIER_DELETE_FOR_USER.value))
        }

        val targetIdentifier = mockk<UserIdentifierInternal> {
            every { userId } returns targetUserId
        }

        val targetUserDetails = mockk<UserDetails> {
            every { id } returns targetUserId
            every { role } returns UserRole.USER
            every { authorityLevel } returns 50
            every { accountStatus } returns UserAccountStatus.ACTIVE
        }

        val identifiersList = listOf(targetIdentifier)

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(managerId) } returns AppResult.Success(managerDetails)
        coEvery { identifierManager.getUserIdentifierByIdForSystem(targetIdentifierId) } returns AppResult.Success(targetIdentifier)
        coEvery { userManager.getUserByIdForSelf(targetUserId) } returns AppResult.Success(targetUserDetails)
        coEvery { identifierManager.getUserIdentifiersByUserId(targetUserId) } returns AppResult.Success(identifiersList)
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())

        val result = useCase(targetIdentifierId, context)

        assertTrue(result is AppResult.Error && result.error is UserError.CannotDeleteUserIdentifier)
    }

    @Test
    fun `returns error when rate limit is exceeded`() = runTest {
        val managerId = UserId.generate()
        val targetIdentifierId = UserIdentifierId.generate()
        val context = createAuthContext(managerId)
        val limitError = mockk<AppError>()

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Error(limitError)
        every { auditErrorConverter.convert(limitError) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())

        val result = useCase(targetIdentifierId, context)

        assertTrue(result is AppResult.Error && result.error == limitError)
        coVerify(exactly = 0) { identifierManager.deleteUserIdentifier(any()) }
    }
}