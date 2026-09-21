package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.unlock

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditErrorLogData
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.lockout.LockoutManager
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.UserOtpVerificationType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutPolicy
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UnlockByEmailUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val auditLogger = mockk<AuditLogger>(relaxed = true)
    private val auditErrorConverter = mockk<AuditErrorConverter>()
    private val securitySettingsProvider = mockk<SecuritySettingsProvider>()
    private val identifierManager = mockk<IdentifierManager>()
    private val otpService = mockk<OtpService>()
    private val userManager = mockk<UserManager>()
    private val lockoutManager = mockk<LockoutManager>()

    private val useCase = UnlockByEmailUseCase(
        rateLimiter = rateLimiter,
        auditLogger = auditLogger,
        auditErrorConverter = auditErrorConverter,
        securitySettingsProvider = securitySettingsProvider,
        identifierManager = identifierManager,
        otpService = otpService,
        userManager = userManager,
        lockoutManager = lockoutManager
    )

    @Test
    fun `successfully unlocks user account by email confirmation code`() = runTest {
        val context = createTestRequestContext()
        val userId = UserId.generate()
        val lockoutPolicy = mockk<AccountLockoutPolicy> {
            every { isSelfServiceUnlockEnabled } returns true
        }
        val identifier = mockk<UserIdentifierInternal> {
            every { this@mockk.userId } returns userId
        }
        val userDetails = mockk<UserDetails> {
            every { id } returns userId
            every { role } returns UserRole.USER
            every { accountStatus } returns UserAccountStatus.ACTIVE
            every { lockoutType } returns AccountLockoutType.NONE
        }

        every { securitySettingsProvider.getAccountLockoutPolicy() } returns lockoutPolicy
        coEvery { rateLimiter.checkRateLimit(UserRateLimitAction.LOGIN_ATTEMPT, TEST_EMAIL) } returns AppResult.Success(Unit)
        coEvery { otpService.verifyOtp(TEST_EMAIL, UserOtpVerificationType.EMAIL_UNLOCK, TEST_CODE) } returns AppResult.Success(true)
        coEvery { identifierManager.getUserIdentifierInternalByProvider(UserAuthProvider.EMAIL, TEST_EMAIL) } returns AppResult.Success(identifier)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { userManager.unlockUserAccount(userId, listOf(TEST_EMAIL)) } returns AppResult.Success(userDetails)

        val result = useCase(TEST_EMAIL, TEST_CODE, context)

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

    @Test
    fun `returns error when self service unlock is disabled`() = runTest {
        val context = createTestRequestContext()
        val lockoutPolicy = mockk<AccountLockoutPolicy> {
            every { isSelfServiceUnlockEnabled } returns false
        }
        val errorLogData = AuditErrorLogData(status = AuditStatus.FAILED, metadata = emptySet())

        every { securitySettingsProvider.getAccountLockoutPolicy() } returns lockoutPolicy
        every { auditErrorConverter.convert(any<UserError.SelfServiceUnlockDisabled>()) } returns errorLogData

        val result = useCase(TEST_EMAIL, TEST_CODE, context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.SelfServiceUnlockDisabled)
    }

    companion object {
        private const val TEST_EMAIL = "test@example.com"
        private const val TEST_CODE = "123456"
    }
}
