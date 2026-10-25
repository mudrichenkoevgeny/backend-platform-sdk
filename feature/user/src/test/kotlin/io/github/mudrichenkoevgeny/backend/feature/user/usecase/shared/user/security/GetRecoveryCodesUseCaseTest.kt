package io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.user.security

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.error.CommonAuditErrorParser
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
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
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.shared.user.security.GetRecoveryCodesUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.DecryptedString
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetRecoveryCodesUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val commonAuditErrorParser = CommonAuditErrorParser()
    private val auditErrorConverter = AuditErrorConverter(emptySet(), commonAuditErrorParser)
    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()
    private val totpManager = mockk<TotpManager>()
    private val authenticationChallengeService = mockk<AuthenticationChallengeService>()

    private val useCase = GetRecoveryCodesUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        userManager = userManager,
        sessionManager = sessionManager,
        totpManager = totpManager,
        authenticationChallengeService = authenticationChallengeService
    )

    private val context = createTestAuthenticatedRequestContext(userRole = UserRole.USER)
    private val userId = context.userId
    private val sessionId = context.sessionId

    @Test
    fun `successfully returns recovery codes`() = runTest {
        val userInternal = createTestUserInternal(id = userId, isTotpEnabled = true)
        val userSessionInternal = createTestUserSessionInternal(id = sessionId, userId = userId)
        val decryptedRecoveryCodes = listOf(DecryptedString("code1"), DecryptedString("code2"))

        coEvery { rateLimiter.checkRateLimit(UserRateLimitAction.USER_GET_RECOVERY_CODES, userId.asHexDashString()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Success(userInternal)
        coEvery { sessionManager.getUserSessionInternal(sessionId) } returns AppResult.Success(userSessionInternal)
        coEvery { authenticationChallengeService.ensureSessionConfirmed(any(), any(), any()) } returns AppResult.Success(Unit)
        coEvery { totpManager.getDecryptedRecoveryCodes(userId) } returns AppResult.Success(decryptedRecoveryCodes)

        val result = useCase(context)

        assertTrue(result is AppResult.Success)
        assertEquals(listOf("code1", "code2"), (result as AppResult.Success).data.codes)

        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.value,
                action = UserAuditActionType.SELF_GET_RECOVERY_CODES,
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when rate limit exceeded`() = runTest {
        val error = CommonError.Unknown()
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Error(error)

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.value,
                action = UserAuditActionType.SELF_GET_RECOVERY_CODES,
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when user not found`() = runTest {
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Success(null)

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserForbidden)
    }

    @Test
    fun `returns error when totp is not enabled`() = runTest {
        val userInternal = createTestUserInternal(id = userId, isTotpEnabled = false)
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Success(userInternal)

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is SecurityError.TotpNotEnabled)
    }

    @Test
    fun `returns error when session confirmation fails`() = runTest {
        val userInternal = createTestUserInternal(id = userId, isTotpEnabled = true)
        val userSessionInternal = createTestUserSessionInternal(id = sessionId, userId = userId)
        val error = CommonError.Unknown()

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Success(userInternal)
        coEvery { sessionManager.getUserSessionInternal(sessionId) } returns AppResult.Success(userSessionInternal)
        coEvery { authenticationChallengeService.ensureSessionConfirmed(any(), any(), any()) } returns AppResult.Error(error)

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)
    }

    @Test
    fun `returns error when decryption fails`() = runTest {
        val userInternal = createTestUserInternal(id = userId, isTotpEnabled = true)
        val userSessionInternal = createTestUserSessionInternal(id = sessionId, userId = userId)
        val error = CommonError.Unknown()

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserInternalById(userId) } returns AppResult.Success(userInternal)
        coEvery { sessionManager.getUserSessionInternal(sessionId) } returns AppResult.Success(userSessionInternal)
        coEvery { authenticationChallengeService.ensureSessionConfirmed(any(), any(), any()) } returns AppResult.Success(Unit)
        coEvery { totpManager.getDecryptedRecoveryCodes(any()) } returns AppResult.Error(error)

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.value,
                action = UserAuditActionType.SELF_GET_RECOVERY_CODES,
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
    }
}
