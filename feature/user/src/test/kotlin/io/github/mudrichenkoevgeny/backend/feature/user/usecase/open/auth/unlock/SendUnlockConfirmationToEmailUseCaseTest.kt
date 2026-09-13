package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.unlock

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpConfirmationData
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.createTestRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.EmailService
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.UserOtpVerificationType
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SendUnlockConfirmationToEmailUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val securitySettingsProvider = mockk<SecuritySettingsProvider>()
    private val identifierManager = mockk<IdentifierManager>()
    private val otpService = mockk<OtpService>()
    private val emailService = mockk<EmailService>()

    private val useCase = SendUnlockConfirmationToEmailUseCase(
        rateLimiter = rateLimiter,
        securitySettingsProvider = securitySettingsProvider,
        identifierManager = identifierManager,
        otpService = otpService,
        emailService = emailService
    )

    @Test
    fun `successfully triggers unlock email flow when user exists`() = runTest {
        val context = createTestRequestContext()
        val otpConfirmation = mockk<OtpConfirmation>()
        val otpData = OtpConfirmationData(code = TEST_CODE, otpConfirmation = otpConfirmation)
        val lockoutPolicy = mockk<AccountLockoutPolicy> {
            every { isSelfServiceUnlockEnabled } returns true
        }
        val identifier = mockk<UserIdentifierInternal>()

        every { securitySettingsProvider.getAccountLockoutPolicy() } returns lockoutPolicy
        coEvery { rateLimiter.checkRateLimit(UserRateLimitAction.SEND_OTP_EMAIL, TEST_EMAIL) } returns AppResult.Success(Unit)
        coEvery { identifierManager.getUserIdentifierInternalByProvider(UserAuthProvider.EMAIL, TEST_EMAIL) } returns AppResult.Success(identifier)
        coEvery { otpService.getOtp(TEST_EMAIL, UserOtpVerificationType.EMAIL_UNLOCK) } returns AppResult.Success(otpData)
        coEvery { emailService.fakeSendEmail() } returns AppResult.Success(Unit)

        val result = useCase(TEST_EMAIL, context)

        assertEquals(AppResult.Success(otpConfirmation), result)
        coVerify(exactly = 1) { emailService.fakeSendEmail() }
    }

    @Test
    fun `sends unlock code when user is not found`() = runTest {
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
        coEvery { emailService.sendUnlockAccountVerificationCode(any(), any(), any()) } returns AppResult.Success(Unit)

        val result = useCase(TEST_EMAIL, context)

        assertEquals(AppResult.Success(otpConfirmation), result)
        coVerify(exactly = 1) { emailService.sendUnlockAccountVerificationCode(TEST_EMAIL, TEST_CODE, any()) }
        coVerify(exactly = 0) { emailService.fakeSendEmail() }
    }

    @Test
    fun `returns error when self service unlock is disabled`() = runTest {
        val context = createTestRequestContext()
        val lockoutPolicy = mockk<AccountLockoutPolicy> {
            every { isSelfServiceUnlockEnabled } returns false
        }

        every { securitySettingsProvider.getAccountLockoutPolicy() } returns lockoutPolicy

        val result = useCase(TEST_EMAIL, context)

        assertTrue(result is AppResult.Error)
        assertTrue((result as AppResult.Error).error is UserError.SelfServiceUnlockDisabled)
    }

    companion object {
        private const val TEST_EMAIL = "test@example.com"
        private const val TEST_CODE = "123456"
    }
}
