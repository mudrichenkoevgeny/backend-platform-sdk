package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.unlock

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpConfirmationData
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.UserOtpVerificationType
import io.github.mudrichenkoevgeny.backend.feature.user.service.phone.PhoneService
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SendUnlockConfirmationToPhoneUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val securitySettingsProvider = mockk<SecuritySettingsProvider>()
    private val identifierManager = mockk<IdentifierManager>()
    private val otpService = mockk<OtpService>()
    private val phoneService = mockk<PhoneService>()
    private val userManager = mockk<UserManager>()

    private val useCase = SendUnlockConfirmationToPhoneUseCase(
        rateLimiter = rateLimiter,
        securitySettingsProvider = securitySettingsProvider,
        identifierManager = identifierManager,
        otpService = otpService,
        phoneService = phoneService,
        userManager = userManager
    )

    @Test
    fun `successfully triggers unlock phone flow when user exists`() = runTest {
        val context = createTestRequestContext()
        val otpConfirmation = mockk<OtpConfirmation>()
        val otpData = OtpConfirmationData(code = TEST_CODE, otpConfirmation = otpConfirmation)
        val lockoutPolicy = mockk<AccountLockoutPolicy> {
            every { isSelfServiceUnlockEnabled } returns true
        }
        val userId = UserId.generate()
        val identifier = mockk<UserIdentifierInternal> {
            every { this@mockk.userId } returns userId
        }
        val userDetails = mockk<UserDetails> {
            every { role } returns UserRole.USER
            every { accountStatus } returns UserAccountStatus.ACTIVE
        }

        every { securitySettingsProvider.getAccountLockoutPolicy() } returns lockoutPolicy
        coEvery { rateLimiter.checkRateLimit(UserRateLimitAction.SEND_OTP_PHONE, TEST_PHONE) } returns AppResult.Success(Unit)
        coEvery { identifierManager.getUserIdentifierInternalByProvider(UserAuthProvider.PHONE, TEST_PHONE) } returns AppResult.Success(identifier)
        coEvery { userManager.getUserByIdForSelf(userId) } returns AppResult.Success(userDetails)
        coEvery { otpService.getOtp(TEST_PHONE, UserOtpVerificationType.PHONE_UNLOCK) } returns AppResult.Success(otpData)
        coEvery { phoneService.sendUnlockAccountVerificationCode(any(), any(), any()) } returns AppResult.Success(Unit)

        val result = useCase(TEST_PHONE, context)

        assertEquals(AppResult.Success(otpConfirmation), result)
        coVerify(exactly = 1) { phoneService.sendUnlockAccountVerificationCode(TEST_PHONE, TEST_CODE, any()) }
        coVerify(exactly = 0) { phoneService.fakeSendSMS() }
    }

    @Test
    fun `sends fake SMS when user is not found`() = runTest {
        val context = createTestRequestContext()
        val otpConfirmation = mockk<OtpConfirmation>()
        val otpData = OtpConfirmationData(code = TEST_CODE, otpConfirmation = otpConfirmation)
        val lockoutPolicy = mockk<AccountLockoutPolicy> {
            every { isSelfServiceUnlockEnabled } returns true
        }

        every { securitySettingsProvider.getAccountLockoutPolicy() } returns lockoutPolicy
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { identifierManager.getUserIdentifierInternalByProvider(any(), any()) } returns AppResult.Success(null)
        coEvery { otpService.getOtp(any(), any()) } returns AppResult.Success(otpData)
        coEvery { phoneService.fakeSendSMS() } returns AppResult.Success(Unit)

        val result = useCase(TEST_PHONE, context)

        assertEquals(AppResult.Success(otpConfirmation), result)
        coVerify(exactly = 1) { phoneService.fakeSendSMS() }
        coVerify(exactly = 0) { phoneService.sendUnlockAccountVerificationCode(any(), any(), any()) }
    }

    @Test
    fun `returns error when self service unlock is disabled`() = runTest {
        val context = createTestRequestContext()
        val lockoutPolicy = mockk<AccountLockoutPolicy> {
            every { isSelfServiceUnlockEnabled } returns false
        }

        every { securitySettingsProvider.getAccountLockoutPolicy() } returns lockoutPolicy

        val result = useCase(TEST_PHONE, context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.SelfServiceUnlockDisabled)
    }

    companion object {
        private const val TEST_PHONE = "+1234567890"
        private const val TEST_CODE = "123456"
    }
}
