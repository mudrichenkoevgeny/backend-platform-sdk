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
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.EmailService
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.UserOtpVerificationType
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendUnlockConfirmationToEmailUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val securitySettingsProvider: SecuritySettingsProvider,
    private val identifierManager: IdentifierManager,
    private val otpService: OtpService,
    private val emailService: EmailService
) {
    /**
     * Initiates self-service account unlock by sending an OTP verification code to the user's email.
     *
     * **Allowed Account Statuses:** Any (Public / Self-service access).
     *
     * **Security:**
     * - Verifies that self-service account unlock is enabled via [SecuritySettingsProvider].
     * - Enforces rate limiting on [UserRateLimitAction.SEND_OTP_EMAIL].
     * - Protects against email enumeration: if the email is not found, simulates email delivery,
     *   returning a successful [OtpConfirmation] response.
     *
     * **Workflow:**
     * 1. Confirms self-service unlock is enabled in account lockout policy.
     * 2. Enforces rate limits for the provided email address.
     * 3. Resolves the email identifier via [IdentifierManager].
     * 4. Generates a new OTP via [OtpService] with [UserOtpVerificationType.EMAIL_UNLOCK].
     * 5. If the identifier exists, sends a real verification email; otherwise simulates delivery.
     *
     * @param email The target email address.
     * @param requestContext Request context containing client details for localization.
     * @return [AppResult] containing [OtpConfirmation] metadata or an error.
     */
    suspend operator fun invoke(
        email: String,
        requestContext: RequestContext
    ): AppResult<OtpConfirmation> {
        val accountLockoutPolicy = securitySettingsProvider.getAccountLockoutPolicy()
        if (!accountLockoutPolicy.isSelfServiceUnlockEnabled) {
            return AppResult.Error(UserError.SelfServiceUnlockDisabled())
        }

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
            type = UserOtpVerificationType.EMAIL_UNLOCK
        )
        val otpConfirmationData = when (getOtpResult) {
            is AppResult.Error -> return AppResult.Error(getOtpResult.error)
            is AppResult.Success -> getOtpResult.data
        }

        return if (identifier != null) {
            sendFakeEmail(otpConfirmationData)
        } else {
            sendConfirmationCode(email, otpConfirmationData, requestContext)
        }
    }

    private suspend fun sendFakeEmail(
        otpConfirmationData: OtpConfirmationData
    ): AppResult<OtpConfirmation> {
        val sendEmailResult = emailService.fakeSendEmail()
        if (sendEmailResult is AppResult.Error) return AppResult.Error(sendEmailResult.error)

        return AppResult.Success(otpConfirmationData.otpConfirmation)
    }

    private suspend fun sendConfirmationCode(
        email: String,
        otpConfirmationData: OtpConfirmationData,
        context: RequestContext
    ): AppResult<OtpConfirmation> {
        val sendEmailResult = emailService.sendUnlockAccountVerificationCode(
            email = email,
            code = otpConfirmationData.code,
            language = context.clientInfo.deviceInfo.language
        )
        if (sendEmailResult is AppResult.Error) return AppResult.Error(sendEmailResult.error)

        return AppResult.Success(otpConfirmationData.otpConfirmation)
    }
}
