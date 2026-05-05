package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpConfirmationData
import io.github.mudrichenkoevgeny.backend.core.security.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.UserOtpVerificationType
import io.github.mudrichenkoevgeny.backend.feature.user.service.phone.PhoneService
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendAddPhoneIdentifierConfirmationUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val identifierManager: IdentifierManager,
    private val otpService: OtpService,
    private val phoneService: PhoneService
) {
    /**
     * Initiates the process of adding a new phone identifier by sending a confirmation code.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY].
     *
     * **Workflow:**
     * 1. Checks rate limits for [UserRateLimitAction.SEND_OTP_PHONE] using the provided phone number.
     * 2. Checks if the phone number is already registered via [IdentifierManager].
     * 3. Generates an OTP code and confirmation data via [OtpService].
     * 4. If the number exists: sends a notification about the existing registration via [PhoneService].
     * 5. If the number is new: sends a verification code via [PhoneService].
     *
     * @param phoneNumber The phone number to be verified and added.
     * @param authenticatedRequestContext The context of the authenticated request.
     * @return [AppResult] containing [OtpConfirmation] data.
     */
    suspend operator fun invoke(
        phoneNumber: String,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<OtpConfirmation> {
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
            type = UserOtpVerificationType.PHONE_VERIFICATION
        )
        val otpConfirmationData = when (getOtpResult) {
            is AppResult.Error -> return AppResult.Error(getOtpResult.error)
            is AppResult.Success -> getOtpResult.data
        }

        return if (identifier != null) {
            sendAlreadyRegistered(phoneNumber, otpConfirmationData, authenticatedRequestContext)
        } else {
            sendConfirmationCode(phoneNumber, otpConfirmationData, authenticatedRequestContext)
        }
    }

    private suspend fun sendAlreadyRegistered(
        phoneNumber: String,
        otpConfirmationData: OtpConfirmationData,
        context: AuthenticatedRequestContext
    ): AppResult<OtpConfirmation> {
        val sendAlreadyRegisteredResult = phoneService.sendAlreadyRegisteredPhoneNumber(
            phoneNumber = phoneNumber,
            ipAddress = context.clientInfo.ipAddress,
            deviceName = context.clientInfo.deviceInfo.deviceName,
            language = context.clientInfo.deviceInfo.language
        )
        if (sendAlreadyRegisteredResult is AppResult.Error) return AppResult.Error(sendAlreadyRegisteredResult.error)

        return AppResult.Success(otpConfirmationData.otpConfirmation)
    }

    private suspend fun sendConfirmationCode(
        phoneNumber: String,
        otpConfirmationData: OtpConfirmationData,
        context: AuthenticatedRequestContext
    ): AppResult<OtpConfirmation> {
        val sendCodeResult = phoneService.sendVerificationCode(
            phoneNumber = phoneNumber,
            code = otpConfirmationData.code,
            language = context.clientInfo.deviceInfo.language
        )
        if (sendCodeResult is AppResult.Error) return AppResult.Error(sendCodeResult.error)

        return AppResult.Success(otpConfirmationData.otpConfirmation)
    }
}