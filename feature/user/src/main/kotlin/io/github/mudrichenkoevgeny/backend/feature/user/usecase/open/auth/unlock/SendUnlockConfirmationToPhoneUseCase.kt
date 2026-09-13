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
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendUnlockConfirmationToPhoneUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val securitySettingsProvider: SecuritySettingsProvider,
    private val identifierManager: IdentifierManager,
    private val otpService: OtpService,
    private val phoneService: PhoneService
) {
    /**
     * Initiates self-service account unlock by sending an OTP verification code to the user's phone.
     *
     * **Allowed Account Statuses:** Any (Public / Self-service access).
     *
     * **Security:**
     * - Verifies that self-service account unlock is enabled via [SecuritySettingsProvider].
     * - Enforces rate limiting on [UserRateLimitAction.SEND_OTP_PHONE].
     * - Protects against account enumeration: if the phone number is not registered, simulates SMS delivery,
     *   returning a successful [OtpConfirmation] response.
     *
     * **Workflow:**
     * 1. Confirms self-service unlock is enabled in account lockout policy.
     * 2. Enforces rate limits for the provided phone number.
     * 3. Resolves the phone identifier via [IdentifierManager].
     * 4. Generates a new OTP via [OtpService] with [UserOtpVerificationType.PHONE_UNLOCK].
     * 5. If the identifier exists, sends a real verification SMS; otherwise simulates delivery.
     *
     * @param phoneNumber Target phone number in E.164 format.
     * @param requestContext Request context containing client details.
     * @return [AppResult] containing [OtpConfirmation] metadata or an error.
     */
    suspend operator fun invoke(
        phoneNumber: String,
        requestContext: RequestContext
    ): AppResult<OtpConfirmation> {
        val accountLockoutPolicy = securitySettingsProvider.getAccountLockoutPolicy()
        if (!accountLockoutPolicy.isSelfServiceUnlockEnabled) {
            return AppResult.Error(UserError.SelfServiceUnlockDisabled())
        }

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.SEND_OTP_PHONE,
            identifier = phoneNumber
        )
        if (rateLimitCheck is AppResult.Error) {
            return AppResult.Error(rateLimitCheck.error)
        }

        val getUserIdentifierResult = identifierManager.getUserIdentifierInternalByProvider(
            userAuthProvider = UserAuthProvider.PHONE,
            identifier = phoneNumber
        )

        val identifier = when (getUserIdentifierResult) {
            is AppResult.Error -> return AppResult.Error(getUserIdentifierResult.error)
            is AppResult.Success -> getUserIdentifierResult.data
        }

        val getOtpResult = otpService.getOtp(
            identifier = phoneNumber,
            type = UserOtpVerificationType.PHONE_UNLOCK
        )
        val otpConfirmationData = when (getOtpResult) {
            is AppResult.Error -> return AppResult.Error(getOtpResult.error)
            is AppResult.Success -> getOtpResult.data
        }

        return if (identifier != null) {
            sendFakeSMS(otpConfirmationData)
        } else {
            sendConfirmationCode(phoneNumber, otpConfirmationData, requestContext)
        }
    }

    private suspend fun sendFakeSMS(
        otpConfirmationData: OtpConfirmationData
    ): AppResult<OtpConfirmation> {
        val sendEmailResult = phoneService.fakeSendSMS()
        if (sendEmailResult is AppResult.Error) return AppResult.Error(sendEmailResult.error)

        return AppResult.Success(otpConfirmationData.otpConfirmation)
    }

    private suspend fun sendConfirmationCode(
        phoneNumber: String,
        otpConfirmationData: OtpConfirmationData,
        context: RequestContext
    ): AppResult<OtpConfirmation> {
        val sendEmailResult = phoneService.sendUnlockAccountVerificationCode(
            phoneNumber = phoneNumber,
            code = otpConfirmationData.code,
            language = context.clientInfo.deviceInfo.language
        )
        if (sendEmailResult is AppResult.Error) return AppResult.Error(sendEmailResult.error)

        return AppResult.Success(otpConfirmationData.otpConfirmation)
    }
}
