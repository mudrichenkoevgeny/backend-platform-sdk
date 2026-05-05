package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.data.AuthData
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
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

private const val TEST_EMAIL = "test@example.com"
private const val TEST_PASSWORD = "password123"

class LoginByEmailUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val authManager = mockk<AuthManager>()

    private val useCase = LoginByEmailUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        authManager = authManager
    )

    private fun createRequestContext() = RequestContext(
        traceId = null,
        userId = null,
        userRole = null,
        sessionId = null,
        clientInfo = ClientInfo()
    )

    @Test
    fun `successfully authenticates user and logs audit`() = runTest {
        val context = createRequestContext()
        val userId = UserId.generate()
        val userDetails = mockk<UserDetails> {
            every { id } returns userId
            every { role } returns UserRole.USER
        }
        val authData = mockk<AuthData> {
            every { this@mockk.userDetails } returns userDetails
        }

        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.LOGIN_ATTEMPT, TEST_EMAIL)
        } returns AppResult.Success(Unit)

        coEvery {
            authManager.authenticateExistingUser(
                clientInfo = context.clientInfo,
                userAuthProvider = UserAuthProvider.EMAIL,
                identifier = TEST_EMAIL,
                password = TEST_PASSWORD
            )
        } returns AppResult.Success(authData)

        val result = useCase(TEST_EMAIL, TEST_PASSWORD, context)

        assertEquals(AppResult.Success(authData), result)

        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.USER.serialName,
                action = UserAuditActionType.LOGIN_BY_EMAIL,
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.SUCCESS,
                message = null,
                metadata = match { meta -> meta.any { it.value == TEST_EMAIL } }
            )
        }
    }

    @Test
    fun `returns error when rate limit exceeded`() = runTest {
        val context = createRequestContext()
        val error = UserError.InvalidCredentials()
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.LOGIN_ATTEMPT, TEST_EMAIL)
        } returns AppResult.Error(error)

        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(TEST_EMAIL, TEST_PASSWORD, context)

        assertTrue(result is AppResult.Error)
        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = null,
                actorType = AuditActorType.USER,
                actorUserRole = null,
                action = UserAuditActionType.LOGIN_BY_EMAIL,
                resource = UserAuditResourceType.USER,
                resourceId = null,
                status = AuditStatus.FAILED,
                message = null,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when authentication fails`() = runTest {
        val context = createRequestContext()
        val authError = UserError.InvalidCredentials()
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery {
            authManager.authenticateExistingUser(any(), any(), any(), any())
        } returns AppResult.Error(authError)

        every { auditErrorConverter.convert(authError) } returns errorLogData

        val result = useCase(TEST_EMAIL, TEST_PASSWORD, context)

        assertTrue(result is AppResult.Error)
        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = null,
                actorType = AuditActorType.USER,
                actorUserRole = null,
                action = UserAuditActionType.LOGIN_BY_EMAIL,
                resource = UserAuditResourceType.USER,
                resourceId = null,
                status = AuditStatus.FAILED,
                message = null,
                metadata = any()
            )
        }
    }
}