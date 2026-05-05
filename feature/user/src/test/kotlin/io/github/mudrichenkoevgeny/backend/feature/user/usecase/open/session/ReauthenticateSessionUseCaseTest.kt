package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.session

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaChallengeData
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaChallengeType
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaService
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.totp.TotpManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReauthenticateSessionUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val mfaService = mockk<MfaService>(relaxed = true)
    private val sessionManager = mockk<SessionManager>()
    private val totpManager = mockk<TotpManager>()

    private val useCase = ReauthenticateSessionUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        mfaService = mfaService,
        sessionManager = sessionManager,
        totpManager = totpManager
    )

    private fun createAuthContext(userId: UserId = UserId.generate(), sessionId: UserSessionId = UserSessionId.generate()) = AuthenticatedRequestContext(
        traceId = null,
        userId = userId,
        userRole = UserRole.USER,
        sessionId = sessionId,
        clientInfo = ClientInfo()
    )

    private val mfaToken = "mfa-token"
    private val totpCode = "123456"

    @Test
    fun `successfully reauthenticates session`() = runTest {
        val userId = UserId.generate()
        val sessionId = UserSessionId.generate()
        val context = createAuthContext(userId, sessionId)

        val mfaChallengeData = MfaChallengeData(
            token = mfaToken,
            userId = userId.asHexDashString(),
            userRole = context.userRole.serialName,
            sessionId = sessionId.asHexDashString(),
            type = MfaChallengeType.STEP_UP
        )

        coEvery { rateLimiter.checkRateLimit(UserRateLimitAction.SESSION_REAUTHENTICATE, any()) } returns AppResult.Success(Unit)
        coEvery { mfaService.getChallenge(mfaToken, MfaChallengeType.STEP_UP) } returns AppResult.Success(mfaChallengeData)
        coEvery { totpManager.verifyTotp(userId, totpCode) } returns AppResult.Success(Unit)
        coEvery { sessionManager.updateLastReauthenticated(sessionId) } returns AppResult.Success(Unit)

        val result = useCase(context, mfaToken, totpCode)

        assertEquals(AppResult.Success(Unit), result)

        coVerify { mfaService.consumeChallenge(mfaToken) }
        coVerify { sessionManager.updateLastReauthenticated(sessionId) }
        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.REAUTHENTICATE_SESSION,
                resource = UserAuditResourceType.SESSION,
                resourceId = sessionId.asHexDashString(),
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when rate limit exceeded`() = runTest {
        val userId = UserId.generate()
        val sessionId = UserSessionId.generate()
        val context = createAuthContext(userId, sessionId)
        val error = mockk<AppError>()

        every { auditErrorConverter.convert(error) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Error(error)

        val result = useCase(context, mfaToken, totpCode)

        assertTrue(result is AppResult.Error)
        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.REAUTHENTICATE_SESSION,
                resource = UserAuditResourceType.SESSION,
                resourceId = sessionId.asHexDashString(),
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when mfa challenge is for different user`() = runTest {
        val userId = UserId.generate()
        val sessionId = UserSessionId.generate()
        val context = createAuthContext(userId, sessionId)

        val wrongMfaChallengeData = MfaChallengeData(
            token = mfaToken,
            userId = UserId.generate().asHexDashString(),
            userRole = context.userRole.serialName,
            sessionId = sessionId.asHexDashString(),
            type = MfaChallengeType.STEP_UP
        )

        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { mfaService.getChallenge(any(), any()) } returns AppResult.Success(wrongMfaChallengeData)

        val result = useCase(context, mfaToken, totpCode)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is SecurityError.InvalidMfaToken)

        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.REAUTHENTICATE_SESSION,
                resource = UserAuditResourceType.SESSION,
                resourceId = sessionId.asHexDashString(),
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when totp verification fails`() = runTest {
        val userId = UserId.generate()
        val sessionId = UserSessionId.generate()
        val context = createAuthContext(userId, sessionId)

        val mfaChallengeData = MfaChallengeData(
            token = mfaToken,
            userId = userId.asHexDashString(),
            userRole = context.userRole.serialName,
            sessionId = sessionId.asHexDashString(),
            type = MfaChallengeType.STEP_UP
        )
        val error = mockk<AppError>()

        every { auditErrorConverter.convert(error) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { mfaService.getChallenge(any(), any()) } returns AppResult.Success(mfaChallengeData)
        coEvery { totpManager.verifyTotp(any(), any()) } returns AppResult.Error(error)

        val result = useCase(context, mfaToken, totpCode)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)

        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.REAUTHENTICATE_SESSION,
                resource = UserAuditResourceType.SESSION,
                resourceId = sessionId.asHexDashString(),
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when session update fails`() = runTest {
        val userId = UserId.generate()
        val sessionId = UserSessionId.generate()
        val context = createAuthContext(userId, sessionId)

        val mfaChallengeData = MfaChallengeData(
            token = mfaToken,
            userId = userId.asHexDashString(),
            userRole = context.userRole.serialName,
            sessionId = sessionId.asHexDashString(),
            type = MfaChallengeType.STEP_UP
        )
        val error = mockk<AppError>()

        every { auditErrorConverter.convert(error) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { mfaService.getChallenge(any(), any()) } returns AppResult.Success(mfaChallengeData)
        coEvery { totpManager.verifyTotp(any(), any()) } returns AppResult.Success(Unit)
        coEvery { sessionManager.updateLastReauthenticated(any()) } returns AppResult.Error(error)

        val result = useCase(context, mfaToken, totpCode)

        assertTrue(result is AppResult.Error)
        coVerify { mfaService.consumeChallenge(mfaToken) }

        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.REAUTHENTICATE_SESSION,
                resource = UserAuditResourceType.SESSION,
                resourceId = sessionId.asHexDashString(),
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
    }
}