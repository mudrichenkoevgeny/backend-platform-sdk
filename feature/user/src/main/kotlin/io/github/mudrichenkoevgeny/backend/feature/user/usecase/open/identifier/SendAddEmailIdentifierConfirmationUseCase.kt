package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpConfirmationData
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.EmailService
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.UserOtpVerificationType
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendAddEmailIdentifierConfirmationUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val identifierManager: IdentifierManager,
    private val otpService: OtpService,
    private val emailService: EmailService
) {
    /**
     * Initiates the process of adding a new email identifier by sending a confirmation code.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY].
     *
     * **Workflow:**
     * 1. Checks rate limits for [UserRateLimitAction.SEND_OTP_EMAIL] using the provided email address.
     * 2. Checks if the email is already registered via [IdentifierManager].
     * 3. Generates an OTP code and confirmation data via [OtpService].
     * 4. If the email exists: sends a notification about the existing registration via [EmailService].
     * 5. If the email is new: sends a verification code via [EmailService].
     *
     * @param email The email address to be verified and added.
     * @param authenticatedRequestContext The context of the authenticated request.
     * @return [AppResult] containing [OtpConfirmation] data.
     */
    suspend operator fun invoke(
        email: String,
        authenticatedRequestContext: AuthenticatedRequestContext
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
            sendAlreadyRegistered(email, otpConfirmationData, authenticatedRequestContext)
        } else {
            sendConfirmationCode(email, otpConfirmationData, authenticatedRequestContext)
        }
    }

    private suspend fun sendAlreadyRegistered(
        email: String,
        otpConfirmationData: OtpConfirmationData,
        context: AuthenticatedRequestContext
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
        context: AuthenticatedRequestContext
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