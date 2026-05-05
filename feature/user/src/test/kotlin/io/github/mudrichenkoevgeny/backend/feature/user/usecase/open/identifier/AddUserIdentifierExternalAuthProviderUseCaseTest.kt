package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.auth.model.ExternalAuthProviderData
import io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier.ExternalAuthVerifier
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.AvailableAuthProviders
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifier
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AddUserIdentifierExternalAuthProviderUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val userManager = mockk<UserManager>()
    private val sessionManager = mockk<SessionManager>()
    private val authManager = mockk<AuthManager>()
    private val authSettingsProvider = mockk<AuthSettingsProvider>()
    private val authenticationChallengeService = mockk<AuthenticationChallengeService>()
    private val externalAuthVerifier = mockk<ExternalAuthVerifier>()

    private lateinit var useCase: AddUserIdentifierExternalAuthProviderUseCase

    @BeforeEach
    fun setUp() {
        every { externalAuthVerifier.provider } returns UserAuthProvider.GOOGLE
        useCase = AddUserIdentifierExternalAuthProviderUseCase(
            rateLimiter = rateLimiter,
            auditLogger = auditLogger,
            auditErrorConverter = auditErrorConverter,
            userManager = userManager,
            sessionManager = sessionManager,
            authManager = authManager,
            externalAuthVerifiers = setOf(externalAuthVerifier),
            authSettingsProvider = authSettingsProvider,
            authenticationChallengeService = authenticationChallengeService
        )
    }

    private fun createAuthContext() = AuthenticatedRequestContext(
        traceId = null,
        userId = UserId.generate(),
        userRole = UserRole.USER,
        sessionId = UserSessionId.generate(),
        clientInfo = ClientInfo()
    )

    @Test
    fun `successfully adds external identifier`() = runTest {
        val context = createAuthContext()
        val userDetails = mockk<UserDetails>()
        val sessionInternal = mockk<UserSessionInternal>()
        val identifierId = UserIdentifierId.generate()
        val identifier = mockk<UserIdentifier> {
            every { id } returns identifierId
        }

        val authData = ExternalAuthProviderData(
            authProvider = UserAuthProvider.GOOGLE,
            externalId = "ext_google_123",
            email = "test@google.com"
        )

        val availableAuthProviders = AvailableAuthProviders(
            primary = listOf(UserAuthProvider.GOOGLE),
            secondary = emptyList()
        )

        coEvery {
            rateLimiter.checkRateLimit(UserRateLimitAction.USER_IDENTIFIER_ADD, any())
        } returns AppResult.Success(Unit)

        every {
            authSettingsProvider.getAvailableAuthProviders()
        } returns availableAuthProviders

        coEvery {
            userManager.getUserByIdForSelf(context.userId)
        } returns AppResult.Success(userDetails)

        coEvery {
            sessionManager.getUserSessionForSystem(context.sessionId)
        } returns AppResult.Success(sessionInternal)

        coEvery {
            authenticationChallengeService.ensureSessionConfirmed(userDetails, sessionInternal)
        } returns AppResult.Success(Unit)

        coEvery { externalAuthVerifier.verify(TEST_TOKEN) } returns AppResult.Success(authData)

        coEvery {
            authManager.createIdentifierForAuthorizedUser(
                userId = context.userId,
                userAuthProvider = UserAuthProvider.GOOGLE,
                identifier = "ext_google_123",
                password = null,
                externalProviderEmail = "test@google.com"
            )
        } returns AppResult.Success(identifier)

        val result = useCase(UserAuthProvider.GOOGLE, TEST_TOKEN, context)

        assertEquals(AppResult.Success(identifier), result)
        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = context.userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.ADD_IDENTIFIER_EXTERNAL_AUTH_PROVIDER,
                resource = UserAuditResourceType.IDENTIFIER,
                resourceId = identifierId.asHexDashString(),
                status = AuditStatus.SUCCESS,
                message = null,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when provider is not enabled in settings`() = runTest {
        val context = createAuthContext()
        val availableAuthProviders = AvailableAuthProviders(
            primary = listOf(UserAuthProvider.EMAIL),
            secondary = emptyList()
        )
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        every { authSettingsProvider.getAvailableAuthProviders() } returns availableAuthProviders
        every { auditErrorConverter.convert(any<UserError.CannotCreateUserIdentifier>()) } returns errorLogData

        val result = useCase(UserAuthProvider.GOOGLE, TEST_TOKEN, context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.CannotCreateUserIdentifier)

        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = context.userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = context.userRole.serialName,
                action = UserAuditActionType.ADD_IDENTIFIER_EXTERNAL_AUTH_PROVIDER,
                resource = UserAuditResourceType.IDENTIFIER,
                resourceId = null,
                status = AuditStatus.FAILED,
                message = null,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when session is not confirmed`() = runTest {
        val context = createAuthContext()
        val userDetails = mockk<UserDetails>()
        val sessionInternal = mockk<UserSessionInternal>()
        val error = UserError.UserForbidden()
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        val availableAuthProviders = AvailableAuthProviders(
            primary = listOf(UserAuthProvider.GOOGLE),
            secondary = emptyList()
        )

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        every { authSettingsProvider.getAvailableAuthProviders() } returns availableAuthProviders
        coEvery { userManager.getUserByIdForSelf(any()) } returns AppResult.Success(userDetails)
        coEvery { sessionManager.getUserSessionForSystem(any()) } returns AppResult.Success(sessionInternal)

        coEvery {
            authenticationChallengeService.ensureSessionConfirmed(any(), any())
        } returns AppResult.Error(error)

        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(UserAuthProvider.GOOGLE, TEST_TOKEN, context)

        assertEquals(AppResult.Error(error), result)
    }

    companion object {
        private const val TEST_TOKEN = "valid_external_token"
    }
}