package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user.security

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.totpcryptoprocessor.TotpCryptoProcessor
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.totp.TotpManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.totp.UserTotpSettings
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.DecryptedString
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.EncryptedString
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
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

class RegenerateRecoveryCodesUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()
    private val totpManager = mockk<TotpManager>()
    private val authenticationChallengeService = mockk<AuthenticationChallengeService>()
    private val totpCryptoProcessor = mockk<TotpCryptoProcessor>()

    private val useCase = RegenerateRecoveryCodesUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        userManager = userManager,
        sessionManager = sessionManager,
        totpManager = totpManager,
        authenticationChallengeService = authenticationChallengeService,
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

    @Test
    fun `successfully regenerates recovery codes`() = runTest {
        val userDetails = mockk<UserDetails> {
            every { isTotpEnabled } returns true
        }
        val userSessionInternal = mockk<UserSessionInternal>()
        val settings = UserTotpSettings(
            userId = userId,
            encryptedSecret = EncryptedString("secret"),
            isConfirmed = true,
            encryptedRecoveryCodes = null,
            lastUsedAt = null
        )
        val newDecryptedCodes = listOf(
            DecryptedString("new1"),
            DecryptedString("new2")
        )

        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.USER_REGENERATE_RECOVERY_CODES, any())
        } returns AppResult.Success(Unit)

        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSystem(sessionId) } returns AppResult.Success(userSessionInternal)
        coEvery { authenticationChallengeService.ensureSessionConfirmed(any(), any()) } returns AppResult.Success(Unit)
        coEvery { totpManager.getSettings(userId) } returns AppResult.Success(settings)
        coEvery { totpCryptoProcessor.generateRecoveryCodes() } returns AppResult.Success(newDecryptedCodes)
        coEvery { totpManager.updateRecoveryCodes(userId, newDecryptedCodes) } returns AppResult.Success(newDecryptedCodes)

        val result = useCase(context)

        assertTrue(result is AppResult.Success)
        assertEquals(listOf("new1", "new2"), (result as AppResult.Success).data.codes)

        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.SELF_REGENERATE_RECOVERY_CODES,
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when rate limit is exceeded`() = runTest {
        val error = mockk<AppError>()
        every { auditErrorConverter.convert(error) } returns AuditErrorLogData(
            status = AuditStatus.FAILED,
            metadata = emptySet()
        )
        coEvery {
            rateLimiter.checkRateLimit(any(), any())
        } returns AppResult.Error(error)

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)

        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.SELF_REGENERATE_RECOVERY_CODES,
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when user not found`() = runTest {
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(null)

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.UserForbidden)
    }

    @Test
    fun `returns error when totp not enabled in user details`() = runTest {
        val userDetails = mockk<UserDetails> {
            every { isTotpEnabled } returns false
        }
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is SecurityError.TotpNotEnabled)
    }

    @Test
    fun `returns error when totp settings are not confirmed`() = runTest {
        val userDetails = mockk<UserDetails> {
            every { isTotpEnabled } returns true
        }
        val settings = UserTotpSettings(
            userId = userId,
            encryptedSecret = EncryptedString("secret"),
            isConfirmed = false,
            encryptedRecoveryCodes = null,
            lastUsedAt = null
        )
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSystem(any()) } returns AppResult.Success(mockk())
        coEvery { authenticationChallengeService.ensureSessionConfirmed(any(), any()) } returns AppResult.Success(Unit)
        coEvery { totpManager.getSettings(userId) } returns AppResult.Success(settings)

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is SecurityError.TotpNotEnabled)
    }

    @Test
    fun `returns error when session confirmation fails`() = runTest {
        val userDetails = mockk<UserDetails> {
            every { isTotpEnabled } returns true
        }
        val error = mockk<AppError>()
        every { auditErrorConverter.convert(error) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSystem(sessionId) } returns AppResult.Success(mockk())
        coEvery { authenticationChallengeService.ensureSessionConfirmed(any(), any()) } returns AppResult.Error(error)

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)
    }

    @Test
    fun `returns error when recovery codes generation fails`() = runTest {
        val userDetails = mockk<UserDetails> {
            every { isTotpEnabled } returns true
        }
        val settings = UserTotpSettings(userId, EncryptedString("s"), true, null, null)
        val error = mockk<AppError>()
        every { auditErrorConverter.convert(error) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSystem(any()) } returns AppResult.Success(mockk())
        coEvery { authenticationChallengeService.ensureSessionConfirmed(any(), any()) } returns AppResult.Success(Unit)
        coEvery { totpManager.getSettings(userId) } returns AppResult.Success(settings)
        coEvery { totpCryptoProcessor.generateRecoveryCodes() } returns AppResult.Error(error)

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)
    }
}