package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.resetpassword

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpConfirmationData
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.EmailService
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.UserOtpVerificationType
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendResetPasswordConfirmationUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val identifierManager: IdentifierManager,
    private val otpService: OtpService,
    private val emailService: EmailService
) {
    /**
     * Initiates the password recovery process by sending an OTP to the user's email.
     *
     * **Allowed Account Statuses:** Any (Public access).
     *
     * **Security:**
     * - Protects against email enumeration by returning a [fakeSendConfirmationCode] response if the email is not found.
     * - Enforces rate limiting on [UserRateLimitAction.SEND_OTP_EMAIL].
     *
     * **Workflow:**
     * 1. Checks rate limits for the provided email address.
     * 2. Verifies if a [UserAuthProvider.EMAIL] identifier exists for the given email.
     * 3. Generates a new OTP via [OtpService] for the [UserOtpVerificationType.EMAIL_PASSWORD_RESET] type.
     * 4. If the user exists, sends a real verification email via [EmailService].
     * 5. If the user does not exist, performs a simulated email sending operation to maintain constant response timing.
     *
     * @param email The email address requesting the password reset.
     * @param requestContext The context of the request, used for localization.
     * @return [AppResult] containing [OtpConfirmation] metadata (expiry, resend intervals).
     */
    suspend operator fun invoke(
        email: String,
        requestContext: RequestContext
    ): AppResult<OtpConfirmation> {
        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.SEND_OTP_EMAIL,
            identifier = email
        )
        if (rateLimitCheck is AppResult.Error) {
            return AppResult.Error(rateLimitCheck.error)
        }

        val getUserIdentifierResult = identifierManager.getUserIdentifierInternalByProvider(
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = email
        )

        val identifier = when (getUserIdentifierResult) {
            is AppResult.Error -> return AppResult.Error(getUserIdentifierResult.error)
            is AppResult.Success -> getUserIdentifierResult.data
        }

        val getOtpResult = otpService.getOtp(
            identifier = email,
            type = UserOtpVerificationType.EMAIL_PASSWORD_RESET
        )
        val otpConfirmationData = when (getOtpResult) {
            is AppResult.Error -> return AppResult.Error(getOtpResult.error)
            is AppResult.Success -> getOtpResult.data
        }

        return if (identifier != null) {
            sendConfirmationCode(email, otpConfirmationData, requestContext)
        } else {
            fakeSendConfirmationCode(otpConfirmationData)
        }
    }

    private suspend fun sendConfirmationCode(
        email: String,
        otpConfirmationData: OtpConfirmationData,
        context: RequestContext
    ): AppResult<OtpConfirmation> {
        val sendEmailResult = emailService.sendResetPasswordVerificationCode(
            email = email,
            code = otpConfirmationData.code,
            language = context.clientInfo.deviceInfo.language
        )
        if (sendEmailResult is AppResult.Error) return AppResult.Error(sendEmailResult.error)

        return AppResult.Success(otpConfirmationData.otpConfirmation)
    }

    private suspend fun fakeSendConfirmationCode(
        otpConfirmationData: OtpConfirmationData
    ): AppResult<OtpConfirmation> {
        val sendEmailResult = emailService.fakeSendEmail()
        if (sendEmailResult is AppResult.Error) return AppResult.Error(sendEmailResult.error)

        return AppResult.Success(otpConfirmationData.otpConfirmation)
    }
}