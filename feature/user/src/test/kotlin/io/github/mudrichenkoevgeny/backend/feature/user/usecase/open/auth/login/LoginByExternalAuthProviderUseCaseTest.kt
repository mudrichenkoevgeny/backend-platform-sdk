package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.auth.model.ExternalAuthProviderData
import io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier.ExternalAuthVerifier
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.data.AuthData
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.PublicAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.ExternalAuthProvider
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

class LoginByExternalAuthProviderUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val authSettingsProvider = mockk<AuthSettingsProvider>()
    private val authManager = mockk<AuthManager>()
    private val externalAuthVerifier = mockk<ExternalAuthVerifier>()

    private val useCase = LoginByExternalAuthProviderUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        externalAuthVerifiers = setOf(externalAuthVerifier),
        authSettingsProvider = authSettingsProvider,
        authManager = authManager
    )

    private fun createRequestContext() = RequestContext(
        traceId = null,
        userId = null,
        userRole = null,
        sessionId = null,
        clientInfo = ClientInfo()
    )

    private fun mockProviderSupport(provider: UserAuthProvider, supported: Boolean) {
        val externalProvider = mockk<ExternalAuthProvider> {
            every { userAuthProvider } returns provider
        }
        val publicSettings = mockk<PublicAuthSettings> {
            every { availableAuthProviders.supportedExternalProviders } returns if (supported) {
                setOf(externalProvider)
            } else {
                emptySet()
            }
        }
        every { authSettingsProvider.getPublicAuthSettings() } returns publicSettings
    }

    @Test
    fun `successfully authenticates via external provider and logs audit`() = runTest {
        val context = createRequestContext()
        val userId = UserId.generate()
        val provider = UserAuthProvider.GOOGLE
        val userDetails = mockk<UserDetails> {
            every { id } returns userId
            every { role } returns UserRole.USER
        }
        val authData = mockk<AuthData> {
            every { this@mockk.userDetails } returns userDetails
        }
        val verificationData = ExternalAuthProviderData(
            authProvider = provider,
            externalId = EXTERNAL_ID,
            email = EXTERNAL_EMAIL
        )

        every { externalAuthVerifier.provider } returns provider
        coEvery { externalAuthVerifier.verify(TEST_TOKEN) } returns AppResult.Success(verificationData)
        coEvery { rateLimiter.checkRateLimit(UserRateLimitAction.LOGIN_ATTEMPT, TEST_TOKEN) } returns AppResult.Success(Unit)
        mockProviderSupport(provider, supported = true)

        coEvery {
            authManager.authenticateOrCreateUser(
                clientInfo = context.clientInfo,
                userAuthProvider = provider,
                identifier = EXTERNAL_ID,
                externalProviderEmail = EXTERNAL_EMAIL
            )
        } returns AppResult.Success(authData)

        val result = useCase(provider, TEST_TOKEN, context)

        assertEquals(AppResult.Success(authData), result)

        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                actorUserRole = UserRole.USER.serialName,
                action = UserAuditActionType.LOGIN_BY_EXTERNAL_AUTH_PROVIDER,
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.SUCCESS,
                message = null,
                metadata = match { meta ->
                    meta.any { it.value == EXTERNAL_ID } && meta.any { it.value == EXTERNAL_EMAIL }
                }
            )
        }
    }

    @Test
    fun `returns error when rate limit is exceeded`() = runTest {
        val context = createRequestContext()
        val error = UserError.InvalidCredentials()
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), TEST_TOKEN) } returns AppResult.Error(error)
        every { auditErrorConverter.convert(error) } returns errorLogData

        val result = useCase(UserAuthProvider.GOOGLE, TEST_TOKEN, context)

        assertTrue(result is AppResult.Error)
        coVerify(exactly = 1) {
            auditLogger.log(
                status = AuditStatus.FAILED,
                action = UserAuditActionType.LOGIN_BY_EXTERNAL_AUTH_PROVIDER,
                resource = UserAuditResourceType.USER,
                actorType = AuditActorType.USER,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when provider is not supported in settings`() = runTest {
        val context = createRequestContext()
        val provider = UserAuthProvider.APPLE
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        every { externalAuthVerifier.provider } returns UserAuthProvider.GOOGLE
        mockProviderSupport(provider, supported = false)
        every { auditErrorConverter.convert(any<UserError.CannotCreateUserIdentifier>()) } returns errorLogData

        val result = useCase(provider, TEST_TOKEN, context)

        assertTrue(result is AppResult.Error)
        coVerify(exactly = 1) {
            auditLogger.log(
                status = AuditStatus.FAILED,
                action = UserAuditActionType.LOGIN_BY_EXTERNAL_AUTH_PROVIDER,
                resource = UserAuditResourceType.USER,
                actorType = AuditActorType.USER,
                metadata = any()
            )
        }
    }

    @Test
    fun `returns error when external verification fails`() = runTest {
        val context = createRequestContext()
        val provider = UserAuthProvider.GOOGLE
        val authError = UserError.InvalidCredentials()
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        every { externalAuthVerifier.provider } returns provider
        mockProviderSupport(provider, supported = true)
        coEvery { externalAuthVerifier.verify(TEST_TOKEN) } returns AppResult.Error(authError)
        every { auditErrorConverter.convert(authError) } returns errorLogData

        val result = useCase(provider, TEST_TOKEN, context)

        assertTrue(result is AppResult.Error)
        coVerify {
            auditLogger.log(
                status = AuditStatus.FAILED,
                action = UserAuditActionType.LOGIN_BY_EXTERNAL_AUTH_PROVIDER,
                resource = UserAuditResourceType.USER,
                actorType = AuditActorType.USER,
                metadata = any()
            )
        }
    }

    companion object {
        private const val TEST_TOKEN = "external_auth_token_123"
        private const val EXTERNAL_ID = "ext_user_789"
        private const val EXTERNAL_EMAIL = "external@example.com"
    }
}