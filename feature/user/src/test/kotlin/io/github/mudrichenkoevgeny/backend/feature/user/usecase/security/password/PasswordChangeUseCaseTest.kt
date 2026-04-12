package io.github.mudrichenkoevgeny.backend.feature.user.usecase.security.password

import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.core.security.passwordhasher.PasswordHasher
import io.github.mudrichenkoevgeny.backend.core.security.usecase.open.passwordpolicy.ValidatePasswordUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.useridentifier.UserIdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class PasswordChangeUseCaseTest {

    private val rateLimiterEnforcer = mockk<RateLimitEnforcer>()
    private val userAuditLogger = mockk<UserAuditLogger>(relaxed = true)
    private val passwordHasher = mockk<PasswordHasher>()
    private val userIdentifierManager = mockk<UserIdentifierManager>()
    private val validatePasswordUseCase = mockk<ValidatePasswordUseCase>()

    private val useCase = PasswordChangeUseCase(
        rateLimiterEnforcer = rateLimiterEnforcer,
        userAuditLogger = userAuditLogger,
        passwordHasher = passwordHasher,
        userIdentifierManager = userIdentifierManager,
        validatePasswordUseCase = validatePasswordUseCase
    )

    @Test
    fun `execute returns error when password policy check fails`() = runBlocking {
        val ctx = requestContext()
        val policyError = AppResult.Error(UserError.InvalidAccessToken())
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns AppResult.Success(Unit)
        every { validatePasswordUseCase(NEW_PASSWORD) } returns policyError

        val result = useCase.execute(
            email = EMAIL,
            newPassword = NEW_PASSWORD,
            oldPassword = OLD_PASSWORD,
            requestContext = ctx
        )

        assertTrue(result is AppResult.Error)
    }

    @Test
    fun `execute returns success when old password valid and update succeeds`() = runBlocking {
        val ctx = requestContext()
        val userIdentifier = testUserIdentifier()
        val updatedIdentifier = userIdentifier.copy(updatedAt = Instant.now())
        coEvery { rateLimiterEnforcer.enforce(any(), any(), any(), any(), any(), any()) } returns AppResult.Success(Unit)
        every { validatePasswordUseCase(NEW_PASSWORD) } returns AppResult.Success(Unit)
        coEvery {
            userIdentifierManager.getUserIdentifier(
                userAuthProvider = UserAuthProvider.EMAIL,
                identifier = EMAIL
            )
        } returns AppResult.Success(userIdentifier)
        coEvery { passwordHasher.isPasswordValid(OLD_PASSWORD, userIdentifier.passwordHash!!) } returns AppResult.Success(true)
        coEvery {
            userIdentifierManager.updateUserIdentifierPassword(
                userIdentifier = userIdentifier,
                identifier = EMAIL,
                password = NEW_PASSWORD
            )
        } returns AppResult.Success(updatedIdentifier)

        val result = useCase.execute(
            email = EMAIL,
            newPassword = NEW_PASSWORD,
            oldPassword = OLD_PASSWORD,
            requestContext = ctx
        )

        assertTrue(result is AppResult.Success)
        assertTrue((result as AppResult.Success).data === updatedIdentifier)
    }

    private fun requestContext() = RequestContext(
        traceId = null,
        userId = UserId.generate(),
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

    private companion object {
        const val EMAIL = "user@example.com"
        const val OLD_PASSWORD = "oldPass123"
        const val NEW_PASSWORD = "newPass456"

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
