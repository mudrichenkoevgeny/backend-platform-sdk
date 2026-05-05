package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.UserOtpVerificationType
import io.github.mudrichenkoevgeny.backend.feature.user.service.phone.PhoneService
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendLoginConfirmationToPhoneUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val otpService: OtpService,
    private val phoneService: PhoneService
) {
    /**
     * Initiates the phone-based login or verification process by sending an OTP via SMS.
     *
     * **Allowed Account Statuses:** Any (Public access).
     *
     * **Security:**
     * - Enforces rate limiting on [UserRateLimitAction.SEND_OTP_PHONE] to prevent SMS flooding and cost exhaustion.
     * - Uses [OtpService] to generate and persist a secure verification code.
     *
     * **Workflow:**
     * 1. Checks rate limits for the provided [phoneNumber].
     * 2. Generates a new OTP for the [UserOtpVerificationType.PHONE_VERIFICATION] type.
     * 3. Dispatches the verification code via [PhoneService], respecting the user's preferred language.
     *
     * @param phoneNumber The target phone number.
     * @param requestContext The context of the request, providing client and device metadata.
     * @return [AppResult] containing [OtpConfirmation] metadata (expiry, resend intervals).
     */
    suspend operator fun invoke(
        phoneNumber: String,
        requestContext: RequestContext
    ): AppResult<OtpConfirmation> {
        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.SEND_OTP_PHONE,
            identifier = phoneNumber
        )
        if (rateLimitCheck is AppResult.Error) {
            return AppResult.Error(rateLimitCheck.error)
        }

        val getOtpResult = otpService.getOtp(
            identifier = phoneNumber,
            type = UserOtpVerificationType.PHONE_VERIFICATION
        )
        val otpConfirmationData = when (getOtpResult) {
            is AppResult.Error -> return AppResult.Error(getOtpResult.error)
            is AppResult.Success -> getOtpResult.data
        }

        val sendCodeResult = phoneService.sendVerificationCode(
            phoneNumber = phoneNumber,
            code = otpConfirmationData.code,
            language = requestContext.clientInfo.deviceInfo.language
        )
        if (sendCodeResult is AppResult.Error) return AppResult.Error(sendCodeResult.error)

        return AppResult.Success(otpConfirmationData.otpConfirmation)
    }
}