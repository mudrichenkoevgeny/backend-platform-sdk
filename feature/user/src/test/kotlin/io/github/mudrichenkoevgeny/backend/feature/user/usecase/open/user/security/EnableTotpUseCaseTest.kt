package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user.security

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
import io.github.mudrichenkoevgeny.backend.core.security.totpcryptoprocessor.TotpCryptoProcessor
import io.github.mudrichenkoevgeny.backend.feature.user.manager.totp.TotpManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.totp.UserTotpSettings
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.DecryptedString
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.EncryptedString
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
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

class EnableTotpUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val userManager = mockk<UserManager>()
    private val totpManager = mockk<TotpManager>()
    private val mfaService = mockk<MfaService>(relaxed = true)
    private val totpCryptoProcessor = mockk<TotpCryptoProcessor>()

    private val useCase = EnableTotpUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        userManager = userManager,
        totpManager = totpManager,
        mfaService = mfaService,
        totpCryptoProcessor = totpCryptoProcessor
    )

    private val userId = UserId.generate()
    private val sessionId = UserSessionId.generate()
    private val context = AuthenticatedRequestContext(
        traceId = null,
        userId = userId,
        userRole = UserRole.USER,
        sessionId = sessionId,
        clientInfo = ClientInfo()
    )

    private val mfaToken = "mfa-token"
    private val code = "123456"

    @Test
    fun `successfully enables totp`() = runTest {
        val userDetails = mockk<UserDetails> {
            every { isTotpEnabled } returns false
        }
        val mfaChallenge = mockk<MfaChallengeData> {
            every { sessionId } returns context.sessionId.asHexDashString()
        }
        val encryptedSecret = EncryptedString("encrypted-secret")
        val settings = UserTotpSettings(
            userId = userId,
            encryptedSecret = encryptedSecret,
            isConfirmed = false,
            encryptedRecoveryCodes = null,
            lastUsedAt = null
        )
        val recoveryCodes = listOf(DecryptedString("code1"), DecryptedString("code2"))

        coEvery { rateLimiter.checkRateLimit(UserRateLimitAction.USER_ENABLE_TOTP, userId.asHexDashString()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { mfaService.getChallenge(mfaToken, MfaChallengeType.SETUP_TOTP) } returns AppResult.Success(mfaChallenge)
        coEvery { totpManager.getSettings(userId) } returns AppResult.Success(settings)
        coEvery { totpCryptoProcessor.isCodeValid(code, encryptedSecret) } returns AppResult.Success(true)
        coEvery { totpCryptoProcessor.generateRecoveryCodes() } returns AppResult.Success(recoveryCodes)
        coEvery { totpManager.confirmTotp(userId, recoveryCodes) } returns AppResult.Success(Unit)
        coEvery { mfaService.consumeChallenge(mfaToken) } returns AppResult.Success(Unit)

        val result = useCase(context, mfaToken, code)

        assertTrue(result is AppResult.Success)
        assertEquals(listOf("code1", "code2"), (result as AppResult.Success).data.codes)

        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.SELF_ENABLE_TOTP,
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when rate limit exceeded`() = runTest {
        val error = mockk<AppError>()
        every { auditErrorConverter.convert(error) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Error(error)

        val result = useCase(context, mfaToken, code)

        assertTrue(result is AppResult.Error)
        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.SELF_ENABLE_TOTP,
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when settings not found`() = runTest {
        val userDetails = mockk<UserDetails> {
            every { isTotpEnabled } returns false
        }
        val mfaChallenge = mockk<MfaChallengeData> {
            every { sessionId } returns context.sessionId.asHexDashString()
        }
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { mfaService.getChallenge(any(), any()) } returns AppResult.Success(mfaChallenge)
        coEvery { totpManager.getSettings(userId) } returns AppResult.Success(null)

        val result = useCase(context, mfaToken, code)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is SecurityError.TotpNotEnabled)
    }

    @Test
    fun `returns error when settings already confirmed`() = runTest {
        val userDetails = mockk<UserDetails> {
            every { isTotpEnabled } returns false
        }
        val mfaChallenge = mockk<MfaChallengeData> {
            every { sessionId } returns context.sessionId.asHexDashString()
        }
        val settings = UserTotpSettings(
            userId = userId,
            encryptedSecret = EncryptedString("secret"),
            isConfirmed = true,
            encryptedRecoveryCodes = null,
            lastUsedAt = null
        )
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { mfaService.getChallenge(any(), any()) } returns AppResult.Success(mfaChallenge)
        coEvery { totpManager.getSettings(userId) } returns AppResult.Success(settings)

        val result = useCase(context, mfaToken, code)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is SecurityError.TotpAlreadyEnabled)
    }

    @Test
    fun `returns error when totp crypto processor fails during validation`() = runTest {
        val userDetails = mockk<UserDetails> {
            every { isTotpEnabled } returns false
        }
        val mfaChallenge = mockk<MfaChallengeData> {
            every { sessionId } returns context.sessionId.asHexDashString()
        }
        val encryptedSecret = EncryptedString("secret")
        val settings = UserTotpSettings(userId, encryptedSecret, false, null, null)
        val error = mockk<AppError>()

        every { auditErrorConverter.convert(error) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(any()) } returns AppResult.Success(userDetails)
        coEvery { mfaService.getChallenge(any(), any()) } returns AppResult.Success(mfaChallenge)
        coEvery { totpManager.getSettings(any()) } returns AppResult.Success(settings)
        coEvery { totpCryptoProcessor.isCodeValid(any(), any()) } returns AppResult.Error(error)

        val result = useCase(context, mfaToken, code)

        assertTrue(result is AppResult.Error)
        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.SELF_ENABLE_TOTP,
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
    }
}