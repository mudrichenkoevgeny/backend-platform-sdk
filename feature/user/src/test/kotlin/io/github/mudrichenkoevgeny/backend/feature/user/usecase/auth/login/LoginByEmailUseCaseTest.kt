package io.github.mudrichenkoevgeny.backend.feature.user.usecase.auth.login

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.core.security.passwordhasher.PasswordHasher
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.useridentifier.UserIdentifierManager
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AccessToken
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthData
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshToken
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.SessionToken
import io.github.mudrichenkoevgeny.backend.feature.user.model.user.User
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class LoginByEmailUseCaseTest {

    private val rateLimiterEnforcer = mockk<RateLimitEnforcer>()
    private val userAuditLogger = mockk<UserAuditLogger>(relaxed = true)
    private val passwordHasher = mockk<PasswordHasher>(relaxed = true)
    private val userIdentifierManager = mockk<UserIdentifierManager>()
    private val authManager = mockk<AuthManager>()

    private val useCase = LoginByEmailUseCase(
        rateLimiterEnforcer = rateLimiterEnforcer,
        userAuditLogger = userAuditLogger,
        passwordHasher = passwordHasher,
        userIdentifierManager = userIdentifierManager,
        authManager = authManager
    )

    @Test
    fun `execute returns rate limit error when enforcer fails`() = runBlocking {
        val ctx = requestContext()
        val rateLimitError = AppResult.Error(UserError.InvalidAccessToken())
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns rateLimitError

        val result = useCase.execute(
            email = EMAIL,
            password = PASSWORD,
            requestContext = ctx
        )

        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `execute returns InvalidCredentials when email not registered`() = runBlocking {
        val ctx = requestContext()
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns AppResult.Success(Unit)
        coEvery {
            userIdentifierManager.getUserIdentifier(
                userAuthProvider = UserAuthProvider.EMAIL,
                identifier = EMAIL
            )
        } returns AppResult.Success(null)

        val result = useCase.execute(
            email = EMAIL,
            password = PASSWORD,
            requestContext = ctx
        )

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.InvalidCredentials)
    }

    @Test
    fun `execute returns success with auth data when credentials valid`() = runBlocking {
        val ctx = requestContext()
        val userIdentifier = testUserIdentifier()
        val authData = testAuthData()
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns AppResult.Success(Unit)
        coEvery {
            userIdentifierManager.getUserIdentifier(
                userAuthProvider = UserAuthProvider.EMAIL,
                identifier = EMAIL
            )
        } returns AppResult.Success(userIdentifier)
        coEvery { passwordHasher.isPasswordValid(PASSWORD, userIdentifier.passwordHash!!) } returns AppResult.Success(true)
        coEvery {
            authManager.provideAuthData(
                userIdentifier = userIdentifier,
                clientInfo = ctx.clientInfo,
                allowedRoles = any()
            )
        } returns AppResult.Success(authData)

        val result = useCase.execute(
            email = EMAIL,
            password = PASSWORD,
            requestContext = ctx
        )

        assertTrue(result is AppResult.Success)
        assertTrue((result as AppResult.Success).data === authData)
    }

    private fun requestContext() = RequestContext(
        traceId = null,
        userId = null,
        sessionId = null,
        clientInfo = CLIENT_INFO
    )

    private fun testUserIdentifier() = UserIdentifier(
        id = UserIdentifierId.generate(),
        userId = UserId.generate(),
        userAuthProvider = UserAuthProvider.EMAIL,
        identifier = EMAIL,
        passwordHash = "hash",
        createdAt = Instant.now(),
        updatedAt = null
    )

    private fun testAuthData() = AuthData(
        currentUser = User(
            id = UserId.generate(),
            role = UserRole.USER,
            accountStatus = UserAccountStatus.ACTIVE,
            lastLoginAt = null,
            lastActiveAt = null,
            createdAt = Instant.now(),
            updatedAt = null
        ),
        sessionToken = SessionToken(
            accessToken = AccessToken("access"),
            refreshToken = RefreshToken("refresh"),
            expiresAt = Instant.now().plusSeconds(3600)
        )
    )

    private companion object {
        const val EMAIL = "user@example.com"
        const val PASSWORD = "password123"

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
