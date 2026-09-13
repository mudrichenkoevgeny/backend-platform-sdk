package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.unlock

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpConfirmationData
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.UserOtpVerificationType
import io.github.mudrichenkoevgeny.backend.feature.user.service.phone.PhoneService
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

class SendUnlockConfirmationToPhoneUseCaseTest {

    private val rateLimiter = mockk<RateLimiter>()
    private val securitySettingsProvider = mockk<SecuritySettingsProvider>()
    private val identifierManager = mockk<IdentifierManager>()
    private val otpService = mockk<OtpService>()
    private val phoneService = mockk<PhoneService>()

    private val useCase = SendUnlockConfirmationToPhoneUseCase(
        rateLimiter = rateLimiter,
        securitySettingsProvider = securitySettingsProvider,
        identifierManager = identifierManager,
        otpService = otpService,
        phoneService = phoneService
    )

    private fun createRequestContext() = RequestContext(
        traceId = null,
        userId = null,
        userRole = null,
        sessionId = null,
        clientInfo = mockk(relaxed = true)
    )

    @Test
    fun `successfully triggers unlock phone flow when user exists`() = runTest {
        val context = createRequestContext()
        val otpConfirmation = mockk<OtpConfirmation>()
        val otpData = OtpConfirmationData(code = TEST_CODE, otpConfirmation = otpConfirmation)
        val lockoutPolicy = mockk<AccountLockoutPolicy> {
            every { isSelfServiceUnlockEnabled } returns true
        }
        val identifier = mockk<UserIdentifierInternal>()

        every { securitySettingsProvider.getAccountLockoutPolicy() } returns lockoutPolicy
        coEvery { rateLimiter.checkRateLimit(UserRateLimitAction.SEND_OTP_PHONE, TEST_PHONE) } returns AppResult.Success(Unit)
        coEvery { identifierManager.getUserIdentifierInternalByProvider(UserAuthProvider.PHONE, TEST_PHONE) } returns AppResult.Success(identifier)
        coEvery { otpService.getOtp(TEST_PHONE, UserOtpVerificationType.PHONE_UNLOCK) } returns AppResult.Success(otpData)
        coEvery { phoneService.fakeSendSMS() } returns AppResult.Success(Unit)

        val result = useCase(TEST_PHONE, context)

        assertEquals(AppResult.Success(otpConfirmation), result)
        coVerify(exactly = 1) { phoneService.fakeSendSMS() }
    }

    @Test
    fun `sends unlock code when user is not found`() = runTest {
        val context = createRequestContext()
        val otpConfirmation = mockk<OtpConfirmation>()
        val otpData = OtpConfirmationData(code = TEST_CODE, otpConfirmation = otpConfirmation)
        val lockoutPolicy = mockk<AccountLockoutPolicy> {
            every { isSelfServiceUnlockEnabled } returns true
        }

        every { securitySettingsProvider.getAccountLockoutPolicy() } returns lockoutPolicy
        coEvery { rateLimiter.checkRateLimit(any(), any()) } returns AppResult.Success(Unit)
        coEvery { identifierManager.getUserIdentifierInternalByProvider(any(), any()) } returns AppResult.Success(null)
        coEvery { otpService.getOtp(any(), any()) } returns AppResult.Success(otpData)
        coEvery { phoneService.sendUnlockAccountVerificationCode(any(), any(), any()) } returns AppResult.Success(Unit)

        val result = useCase(TEST_PHONE, context)

        assertEquals(AppResult.Success(otpConfirmation), result)
        coVerify(exactly = 1) { phoneService.sendUnlockAccountVerificationCode(TEST_PHONE, TEST_CODE, any()) }
        coVerify(exactly = 0) { phoneService.fakeSendSMS() }
    }

    @Test
    fun `returns error when self service unlock is disabled`() = runTest {
        val context = createRequestContext()
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
