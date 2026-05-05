package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.register

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
class SendRegistrationConfirmationToEmailUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val identifierManager: IdentifierManager,
    private val otpService: OtpService,
    private val emailService: EmailService
) {
    /**
     * Initiates the email registration process by sending an OTP verification code.
     *
     * **Allowed Account Statuses:** Any (Public access).
     *
     * **Security:**
     * - Protects against email enumeration: if the email is already registered, it sends an "already registered"
     *   notification instead of a verification code, but returns the same successful response structure.
     * - Enforces rate limiting on [UserRateLimitAction.SEND_OTP_EMAIL].
     *
     * **Workflow:**
     * 1. Checks rate limits for the target email address.
     * 2. Checks if the [email] is already associated with an existing [UserAuthProvider.EMAIL] identifier.
     * 3. Generates a new OTP via [OtpService] for the [UserOtpVerificationType.EMAIL_VERIFICATION] type.
     * 4. If the email is new, sends a registration verification code via [EmailService].
     * 5. If the email exists, sends a security notification via [EmailService] informing the user of the attempt.
     *
     * @param email The email address to be registered.
     * @param requestContext The context of the request, used for localization and device info.
     * @return [AppResult] containing [OtpConfirmation] metadata.
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
            type = UserOtpVerificationType.EMAIL_VERIFICATION
        )
        val otpConfirmationData = when (getOtpResult) {
            is AppResult.Error -> return AppResult.Error(getOtpResult.error)
            is AppResult.Success -> getOtpResult.data
        }

        return if (identifier != null) {
            sendAlreadyRegistered(email, otpConfirmationData, requestContext)
        } else {
            sendConfirmationCode(email, otpConfirmationData, requestContext)
        }
    }

    private suspend fun sendAlreadyRegistered(
        email: String,
        otpConfirmationData: OtpConfirmationData,
        context: RequestContext
    ): AppResult<OtpConfirmation> {
        val sendEmailResult = emailService.sendAlreadyRegisteredEmail(
            email = email,
            ipAddress = context.clientInfo.ipAddress,
            deviceName = context.clientInfo.deviceInfo.deviceName,
            language = context.clientInfo.deviceInfo.language
        )
        if (sendEmailResult is AppResult.Error) return AppResult.Error(sendEmailResult.error)

        return AppResult.Success(otpConfirmationData.otpConfirmation)
    }

    private suspend fun sendConfirmationCode(
        email: String,
        otpConfirmationData: OtpConfirmationData,
        context: RequestContext
    ): AppResult<OtpConfirmation> {
        val sendEmailResult = emailService.sendVerificationCode(
            email = email,
            code = otpConfirmationData.code,
            language = context.clientInfo.deviceInfo.language
        )
        if (sendEmailResult is AppResult.Error) return AppResult.Error(sendEmailResult.error)

        return AppResult.Success(otpConfirmationData.otpConfirmation)
    }
}