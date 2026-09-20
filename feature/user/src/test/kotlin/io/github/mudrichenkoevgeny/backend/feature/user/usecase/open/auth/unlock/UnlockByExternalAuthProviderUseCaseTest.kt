package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.unlock

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.auth.model.ExternalAuthProviderData
import io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier.ExternalAuthVerifier
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutPolicy
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.AvailableAuthProviders
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.OpenAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.ExternalAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UnlockByExternalAuthProviderUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val externalAuthVerifier = mockk<ExternalAuthVerifier> {
        every { provider } returns UserAuthProvider.GOOGLE
    }
    private val authSettingsProvider = mockk<AuthSettingsProvider>()
    private val securitySettingsProvider = mockk<SecuritySettingsProvider>()
    private val identifierManager = mockk<IdentifierManager>()
    private val userManager = mockk<UserManager>()

    private val useCase = UnlockByExternalAuthProviderUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        externalAuthVerifiers = setOf(externalAuthVerifier),
        authSettingsProvider = authSettingsProvider,
        securitySettingsProvider = securitySettingsProvider,
        identifierManager = identifierManager,
        userManager = userManager
    )

    @Test
    fun `successfully unlocks user account by external auth provider token`() = runTest {
        val context = createTestRequestContext()
        val userId = UserId.generate()
        val lockoutPolicy = mockk<AccountLockoutPolicy> {
            every { isSelfServiceUnlockEnabled } returns true
        }
        val externalProvider = mockk<ExternalAuthProvider> {
            every { userAuthProvider } returns UserAuthProvider.GOOGLE
        }
        val availableAuthProviders = mockk<AvailableAuthProviders> {
            every { supportedExternalProviders } returns setOf(externalProvider)
        }
        val openAuthSettings = mockk<OpenAuthSettings> {
            every { this@mockk.availableAuthProviders } returns availableAuthProviders
        }
        val verificationData = ExternalAuthProviderData(
            authProvider = UserAuthProvider.GOOGLE,
            externalId = TEST_EXTERNAL_ID,
            email = null
        )
        val identifier = mockk<UserIdentifierInternal> {
            every { this@mockk.userId } returns userId
        }
        val userDetails = mockk<UserDetails> {
            every { id } returns userId
            every { role } returns UserRole.USER
            every { accountStatus } returns UserAccountStatus.ACTIVE
        }

        every { securitySettingsProvider.getAccountLockoutPolicy() } returns lockoutPolicy
        every { authSettingsProvider.getOpenAuthSettings() } returns openAuthSettings
        coEvery { rateLimiter.checkRateLimit(UserRateLimitAction.LOGIN_ATTEMPT, TEST_TOKEN) } returns AppResult.Success(Unit)
        coEvery { externalAuthVerifier.verify(TEST_TOKEN) } returns AppResult.Success(verificationData)
        coEvery { identifierManager.getUserIdentifierInternalByProvider(UserAuthProvider.GOOGLE, TEST_EXTERNAL_ID) } returns AppResult.Success(identifier)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { userManager.unlockUserAccount(userId, listOf(TEST_EXTERNAL_ID)) } returns AppResult.Success(userDetails)

        val result = useCase(UserAuthProvider.GOOGLE, TEST_TOKEN, context)

        assertEquals(AppResult.Success(Unit), result)
        coVerify(exactly = 1) {
            auditLogger.log(
                actorId = userId.asHexDashString(),
                actorType = AuditActorType.USER,
                action = UserAuditActionType.SELF_UNLOCK_ACCOUNT,
                resource = UserAuditResourceType.USER,
                resourceId = userId.asHexDashString(),
                status = AuditStatus.SUCCESS,
                metadata = any()
            )
        }
    }

    companion object {
        private const val TEST_TOKEN = "google_token"
        private const val TEST_EXTERNAL_ID = "ext_123"
    }
}
