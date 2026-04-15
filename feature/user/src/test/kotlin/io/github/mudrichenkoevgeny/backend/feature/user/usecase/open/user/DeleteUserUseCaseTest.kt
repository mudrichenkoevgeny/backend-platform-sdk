package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserSessionId
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.authenticationpolicychecker.AuthenticationPolicyChecker
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshTokenHash
import io.github.mudrichenkoevgeny.backend.feature.user.model.session.UserSession
import io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user.DeleteUserUseCase
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class DeleteUserUseCaseTest {

    private val rateLimiterEnforcer = mockk<RateLimitEnforcer>()
    private val userAuditLogger = mockk<UserAuditLogger>(relaxed = true)
    private val sessionManager = mockk<SessionManager>()
    private val userManager = mockk<UserManager>()
    private val authenticationPolicyChecker = mockk<AuthenticationPolicyChecker>()

    private val useCase = DeleteUserUseCase(
        rateLimiterEnforcer = rateLimiterEnforcer,
        userAuditLogger = userAuditLogger,
        sessionManager = sessionManager,
        userManager = userManager,
        authenticationPolicyChecker = authenticationPolicyChecker
    )

    @Test
    fun `execute returns InvalidAccessToken when request context has no userId`() = runBlocking {
        val ctx = requestContext(userId = null, sessionId = UserSessionId.generate())
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns AppResult.Success(Unit)

        val result = useCase.execute(userId = UserId.generate(), requestContext = ctx)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.InvalidAccessToken)
    }

    @Test
    fun `execute returns InvalidSession when request context has no sessionId`() = runBlocking {
        val userId = UserId.generate()
        val ctx = requestContext(userId = userId, sessionId = null)

        val result = useCase.execute(userId = userId, requestContext = ctx)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.InvalidSession)
    }

    @Test
    fun `execute returns InvalidAccessToken when userId does not match current user`() = runBlocking {
        val currentUserId = UserId.generate()
        val otherUserId = UserId.generate()
        val ctx = requestContext(userId = currentUserId, sessionId = UserSessionId.generate())

        val result = useCase.execute(userId = otherUserId, requestContext = ctx)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.InvalidAccessToken)
    }

    @Test
    fun `execute returns AuthenticationConfirmationRequired when re-auth not recent`() = runBlocking {
        val userId = UserId.generate()
        val sessionId = UserSessionId.generate()
        val ctx = requestContext(userId = userId, sessionId = sessionId)
        val sessions = listOf(testUserSession(sessionId, userId))
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns AppResult.Success(Unit)
        coEvery { sessionManager.getAllUserSessions(userId) } returns AppResult.Success(sessions)
        coEvery { authenticationPolicyChecker.isAuthenticationConfirmedRecently(any()) } returns false

        val result = useCase.execute(userId = userId, requestContext = ctx)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is SecurityError.AuthenticationConfirmationRequired)
    }

    @Test
    fun `execute returns success and deletes user when re-auth recent`() = runBlocking {
        val userId = UserId.generate()
        val sessionId = UserSessionId.generate()
        val ctx = requestContext(userId = userId, sessionId = sessionId)
        val sessions = listOf(testUserSession(sessionId, userId))
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns AppResult.Success(Unit)
        coEvery { sessionManager.getAllUserSessions(userId) } returns AppResult.Success(sessions)
        coEvery { authenticationPolicyChecker.isAuthenticationConfirmedRecently(any()) } returns true
        coEvery { userManager.deleteUserById(userId) } returns AppResult.Success(Unit)

        val result = useCase.execute(userId = userId, requestContext = ctx)

        assertTrue(result is AppResult.Success)
    }

    private fun testUserSession(sessionId: UserSessionId, userId: UserId, lastReauthenticatedAt: Instant = Instant.now()) =
        UserSession(
            id = sessionId,
            userId = userId,
            userIdentifierId = UserIdentifierId.generate(),
            userIdentifierAuthProvider = UserAuthProvider.EMAIL,
            refreshTokenHash = RefreshTokenHash("hash"),
            expiresAt = Instant.now().plusSeconds(3600),
            revoked = false,
            userClientType = null,
            userAgent = null,
            ipAddress = null,
            language = null,
            userDeviceId = null,
            userDeviceName = null,
            appVersion = null,
            operationSystemVersion = null,
            createdAt = Instant.now(),
            updatedAt = null,
            lastAccessedAt = Instant.now(),
            lastReauthenticatedAt = lastReauthenticatedAt
        )

    private fun requestContext(userId: UserId?, sessionId: UserSessionId?) = RequestContext(
        traceId = null,
        userId = userId,
        sessionId = sessionId,
        clientInfo = CLIENT_INFO
    )

    private companion object {
        val CLIENT_INFO = ClientInfo(
            clientType = null,
            userAgent = null,
            ipAddress = null,
            language = null,
            host = null,
            origin = null,
            deviceId = null,
            deviceName = null,
            appVersion = null,
            operationSystemVersion = null
        )
    }
}
