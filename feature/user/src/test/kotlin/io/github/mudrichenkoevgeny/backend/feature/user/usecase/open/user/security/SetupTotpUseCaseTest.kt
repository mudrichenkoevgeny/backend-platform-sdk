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
import io.github.mudrichenkoevgeny.backend.core.security.totpcryptoprocessor.model.GeneratedTotpSecret
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.totp.TotpManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.DecryptedString
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.crypt.EncryptedString
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
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

class SetupTotpUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()
    private val identifierManager = mockk<IdentifierManager>()
    private val totpManager = mockk<TotpManager>()
    private val mfaService = mockk<MfaService>()
    private val totpCryptoProcessor = mockk<TotpCryptoProcessor>()

    private val useCase = SetupTotpUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        userManager = userManager,
        sessionManager = sessionManager,
        identifierManager = identifierManager,
        totpManager = totpManager,
        mfaService = mfaService,
        totpCryptoProcessor = totpCryptoProcessor
    )

    private val userId = UserId.generate()
    private val sessionId = UserSessionId.generate()
    private val identifierId = UserIdentifierId.generate()
    private val context = AuthenticatedRequestContext(
        traceId = null,
        userId = userId,
        userRole = UserRole.USER,
        sessionId = sessionId,
        clientInfo = ClientInfo()
    )

    @Test
    fun `successfully initiates totp setup`() = runTest {
        val userDetails = mockk<UserDetails> {
            every { isTotpEnabled } returns false
            every { role } returns UserRole.USER
        }

        val userSession = mockk<UserSessionInternal> {
            every { identifierAuthProvider } returns UserAuthProvider.EMAIL
            every { identifier } returns "test@example.com"
            every { identifierId } returns this@SetupTotpUseCaseTest.identifierId
        }

        val generatedTotp = GeneratedTotpSecret(
            decryptedSecret = DecryptedString("secret"),
            encryptedSecret = EncryptedString("enc-secret"),
            otpAuthUrl = "otpauth://test"
        )

        val mfaChallengeData = MfaChallengeData(
            token = "mfa-token",
            userId = userId.asHexDashString(),
            userRole = UserRole.USER.serialName,
            identifierId = identifierId.asHexDashString(),
            sessionId = sessionId.asHexDashString(),
            type = MfaChallengeType.SETUP_TOTP
        )

        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.USER_SETUP_TOTP, any())
        } returns AppResult.Success(Unit)

        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSystem(sessionId) } returns AppResult.Success(userSession)
        coEvery { totpCryptoProcessor.generateNewSecret("test@example.com") } returns AppResult.Success(generatedTotp)
        coEvery { totpManager.initiateTotpSetup(userId, generatedTotp.encryptedSecret) } returns AppResult.Success(mockk())

        coEvery {
            mfaService.createChallenge(
                userId = userId.asHexDashString(),
                userRole = UserRole.USER.serialName,
                type = MfaChallengeType.SETUP_TOTP,
                identifierId = identifierId.asHexDashString(),
                sessionId = sessionId.asHexDashString()
            )
        } returns AppResult.Success(mfaChallengeData)

        val result = useCase(context)

        assertTrue(result is AppResult.Success)
        with(result as AppResult.Success) {
            assertEquals("secret", data.secretKey)
            assertEquals("otpauth://test", data.otpAuthUrl)
            assertEquals("mfa-token", data.mfaToken)
        }

        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.SELF_SETUP_TOTP_INITIATED,
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

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        assertEquals(error, (result as AppResult.Error).error)

        coVerify {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.SELF_SETUP_TOTP_INITIATED,
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.FAILED,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when totp is already enabled`() = runTest {
        val userDetails = mockk<UserDetails> {
            every { isTotpEnabled } returns true
        }
        every { auditErrorConverter.convert(any()) } returns AuditErrorLogData(AuditStatus.FAILED, emptySet())
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)

        val result = useCase(context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is SecurityError.TotpAlreadyEnabled)
    }

    @Test
    fun `successfully handles external auth provider with email`() = runTest {
        val userDetails = mockk<UserDetails> {
            every { isTotpEnabled } returns false
            every { role } returns UserRole.USER
        }

        val userSession = mockk<UserSessionInternal> {
            every { identifierAuthProvider } returns UserAuthProvider.GOOGLE
            every { identifier } returns "external-id"
            every { identifierId } returns this@SetupTotpUseCaseTest.identifierId
        }

        val identifierInternal = mockk<UserIdentifierInternal> {
            every { externalProviderEmail } returns "google-user@gmail.com"
        }

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSystem(sessionId) } returns AppResult.Success(userSession)
        coEvery { identifierManager.getUserIdentifierByIdForSystem(identifierId) } returns AppResult.Success(identifierInternal)

        coEvery {
            totpCryptoProcessor.generateNewSecret("Google: google-user@gmail.com")
        } returns AppResult.Success(
            GeneratedTotpSecret(DecryptedString("s"), EncryptedString("e"), "url")
        )

        coEvery { totpManager.initiateTotpSetup(any(), any()) } returns AppResult.Success(mockk())

        coEvery {
            mfaService.createChallenge(any(), any(), any(), any(), any())
        } returns AppResult.Success(
            MfaChallengeData(
                token = "t",
                userId = "id",
                userRole = "r",
                type = MfaChallengeType.SETUP_TOTP
            )
        )

        val result = useCase(context)

        assertTrue(result is AppResult.Success)

        coVerify {
            totpCryptoProcessor.generateNewSecret("Google: google-user@gmail.com")
        }
    }
}