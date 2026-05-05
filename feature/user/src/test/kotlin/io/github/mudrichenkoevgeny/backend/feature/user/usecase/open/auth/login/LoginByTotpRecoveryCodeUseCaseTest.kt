package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaChallengeData
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaChallengeType
import io.github.mudrichenkoevgeny.backend.core.security.service.mfa.MfaService
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.totp.TotpManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.data.AuthData
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
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

class LoginByTotpRecoveryCodeUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val mfaService = mockk<MfaService>()
    private val totpManager = mockk<TotpManager>()
    private val authManager = mockk<AuthManager>()

    private val useCase = LoginByTotpRecoveryCodeUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        mfaService = mfaService,
        totpManager = totpManager,
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
    fun `successfully authenticates by recovery code and logs audit`() = runTest {
        val context = createRequestContext()
        val userId = UserId.generate()
        val identifierId = UserIdentifierId.generate()
        val userDetails = mockk<UserDetails> {
            every { id } returns userId
            every { role } returns UserRole.USER
        }
        val authData = mockk<AuthData> {
            every { this@mockk.userDetails } returns userDetails
        }
        val mfaChallenge = MfaChallengeData(
            token = TEST_MFA_TOKEN,
            userId = userId.asHexDashString(),
            userRole = UserRole.USER.serialName,
            identifierId = identifierId.asHexDashString(),
            type = MfaChallengeType.LOGIN_TOTP
        )

        coEvery { rateLimiter.checkRateLimit(UserRateLimitAction.LOGIN_ATTEMPT, TEST_MFA_TOKEN) } returns AppResult.Success(Unit)
        coEvery { mfaService.getChallenge(TEST_MFA_TOKEN, MfaChallengeType.LOGIN_TOTP) } returns AppResult.Success(mfaChallenge)
        coEvery { totpManager.verifyTotpRecoveryCode(userId, TEST_RECOVERY_CODE) } returns AppResult.Success(Unit)
        coEvery { mfaService.consumeChallenge(TEST_MFA_TOKEN) } returns AppResult.Success(Unit)
        coEvery { authManager.completeMfaAuthentication(userId, identifierId, any()) } returns AppResult.Success(authData)

        val result = useCase(context, TEST_MFA_TOKEN, TEST_RECOVERY_CODE)

        assertEquals(AppResult.Success(authData), result)

        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.USER.serialName,
                action = UserAuditActionType.LOGIN_BY_TOTP_RECOVERY_CODE, // action на месте
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.SUCCESS,
                message = null,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when recovery code is wrong`() = runTest {
        val context = createRequestContext()
        val userId = UserId.generate()
        val error = UserError.WrongConfirmationCode()
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())
        val mfaChallenge = MfaChallengeData(
            token = TEST_MFA_TOKEN,
            userId = userId.asHexDashString(),
            userRole = UserRole.USER.serialName,
            identifierId = UserIdentifierId.generate().asHexDashString(),
            type = MfaChallengeType.LOGIN_TOTP
        )

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { mfaService.getChallenge(any(), any()) } returns AppResult.Success(mfaChallenge)
        coEvery { totpManager.verifyTotpRecoveryCode(any(), any()) } returns AppResult.Error(error)
        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(context, TEST_MFA_TOKEN, TEST_RECOVERY_CODE)

        assertTrue(result is AppResult.Error)
        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.USER.serialName,
                action = UserAuditActionType.LOGIN_BY_TOTP_RECOVERY_CODE,
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.FAILED,
                message = null,
                metadata = any()
            )
        }
        coVerify(exactly = 0) { mfaService.consumeChallenge(any()) }
    }

    @Test
    fun `returns error when mfa token is invalid`() = runTest {
        val context = createRequestContext()
        val error = SecurityError.InvalidMfaToken()
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { mfaService.getChallenge(any(), any()) } returns AppResult.Error(error)
        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(context, TEST_MFA_TOKEN, TEST_RECOVERY_CODE)

        assertTrue(result is AppResult.Error)
        coVerify(exactly = 1) {
            auditLogger.log(
                action = UserAuditActionType.LOGIN_BY_TOTP_RECOVERY_CODE,
                status = AuditStatus.FAILED,
                actorType = AuditActorType.USER,
                resource = UserAuditResourceType.USER,
                metadata = any()
            )
        }
    }

    companion object {
        private const val TEST_MFA_TOKEN = "mfa_session_token_abc"
        private const val TEST_RECOVERY_CODE = "ABCD-1234-EFGH"
    }
}