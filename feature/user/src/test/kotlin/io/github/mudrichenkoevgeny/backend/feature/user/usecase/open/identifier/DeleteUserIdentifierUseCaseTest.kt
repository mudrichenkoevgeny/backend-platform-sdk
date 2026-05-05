package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeleteUserIdentifierUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()
    private val identifierManager = mockk<IdentifierManager>()
    private val webSocketManager = mockk<WebSocketManager>(relaxed = true)
    private val authenticationChallengeService = mockk<AuthenticationChallengeService>()

    private val useCase = DeleteUserIdentifierUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        userManager = userManager,
        sessionManager = sessionManager,
        identifierManager = identifierManager,
        webSocketManager = webSocketManager,
        authenticationChallengeService = authenticationChallengeService
    )

    private fun createAuthContext() = AuthenticatedRequestContext(
        traceId = null,
        userId = UserId.generate(),
        userRole = UserRole.USER,
        sessionId = UserSessionId.generate(),
        clientInfo = ClientInfo()
    )

    @Test
    fun `successfully deletes identifier and notifies associated sessions`() = runTest {
        val context = createAuthContext()
        val userDetails = mockk<UserDetails>()
        val currentSession = mockk<UserSessionInternal>()

        val idToDelete = UserIdentifierId.generate()
        val otherId = UserIdentifierId.generate()

        val identifiers = listOf(
            mockk<UserIdentifierInternal> { every { id } returns idToDelete },
            mockk<UserIdentifierInternal> { every { id } returns otherId }
        )

        val sessionToNotifyId = UserSessionId.generate()
        val sessionToNotify = mockk<UserSessionInternal> {
            every { id } returns sessionToNotifyId
            every { identifierId } returns idToDelete
        }
        val otherSessionId = UserSessionId.generate()
        val otherSession = mockk<UserSessionInternal> {
            every { id } returns otherSessionId
            every { identifierId } returns otherId
        }

        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.USER_IDENTIFIER_DELETE, any())
        } returns AppResult.Success(Unit)

        coEvery { userManager.getUserByIdForSelf(context.userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSystem(context.sessionId) } returns AppResult.Success(currentSession)
        coEvery { authenticationChallengeService.ensureSessionConfirmed(any(), any()) } returns AppResult.Success(Unit)

        coEvery { identifierManager.getUserIdentifiersByUserId(context.userId) } returns AppResult.Success(identifiers)
        coEvery { sessionManager.getAllUserSessions(context.userId) } returns AppResult.Success(listOf(sessionToNotify, otherSession))

        coEvery { identifierManager.deleteUserIdentifier(idToDelete) } returns AppResult.Success(Unit)

        val result = useCase(idToDelete, context)

        assertEquals(AppResult.Success(Unit), result)

        coVerify(exactly = 1) { webSocketManager.sendMessageToUserSession(sessionToNotifyId, any()) }
        coVerify(exactly = 0) { webSocketManager.sendMessageToUserSession(otherSessionId, any()) }

        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = context.userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.SELF_DELETE_IDENTIFIER,
                resource = UserAuditResourceType.IDENTIFIER,
                resourceId = idToDelete.asHexDashString(),
                status = AuditStatus.SUCCESS,
                message = null,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when trying to delete the last identifier`() = runTest {
        val context = createAuthContext()
        val userDetails = mockk<UserDetails>()
        val currentSession = mockk<UserSessionInternal>()
        val idToDelete = UserIdentifierId.generate()

        val identifiers = listOf(
            mockk<UserIdentifierInternal> { every { id } returns idToDelete }
        )
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(any()) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSystem(any()) } returns AppResult.Success(currentSession)
        coEvery { authenticationChallengeService.ensureSessionConfirmed(any(), any()) } returns AppResult.Success(Unit)
        coEvery { identifierManager.getUserIdentifiersByUserId(any()) } returns AppResult.Success(identifiers)

        every { auditErrorConverter.convert(any<UserError.CannotDeleteUserIdentifier>()) } returns errorLogData

        val result = useCase(idToDelete, context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.CannotDeleteUserIdentifier)

        coVerify(exactly = 0) { identifierManager.deleteUserIdentifier(any()) }
    }

    @Test
    fun `returns error when identifier not found in user list`() = runTest {
        val context = createAuthContext()
        val userDetails = mockk<UserDetails>()
        val currentSession = mockk<UserSessionInternal>()
        val unknownId = UserIdentifierId.generate()
        val existingId = UserIdentifierId.generate()
        val existingId2 = UserIdentifierId.generate()

        val identifiers = listOf(
            mockk<UserIdentifierInternal> { every { id } returns existingId },
            mockk<UserIdentifierInternal> { every { id } returns existingId2 }
        )
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(any()) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSystem(any()) } returns AppResult.Success(currentSession)
        coEvery { authenticationChallengeService.ensureSessionConfirmed(any(), any()) } returns AppResult.Success(Unit)
        coEvery { identifierManager.getUserIdentifiersByUserId(any()) } returns AppResult.Success(identifiers)

        every { auditErrorConverter.convert(any<UserError.CannotDeleteUserIdentifier>()) } returns errorLogData

        val result = useCase(unknownId, context)

        assertTrue(result is AppResult.Error)
        coVerify(exactly = 0) { identifierManager.deleteUserIdentifier(any()) }
    }
}