package com.mudrichenkoevgeny.backend.feature.user.usecase.auth.password

import com.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import com.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitAction
import com.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import com.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import com.mudrichenkoevgeny.backend.feature.user.enums.OtpVerificationType
import com.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import com.mudrichenkoevgeny.backend.feature.user.manager.useridentifier.UserIdentifierManager
import com.mudrichenkoevgeny.backend.feature.user.model.confirmation.SendConfirmation
import com.mudrichenkoevgeny.backend.feature.user.service.email.EmailService
import com.mudrichenkoevgeny.backend.feature.user.service.otp.OtpService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendResetPasswordConfirmationUseCase @Inject constructor(
    private val rateLimiterEnforcer: RateLimitEnforcer,
    private val userAuditLogger: UserAuditLogger,
    private val otpService: OtpService,
    private val emailService: EmailService,
    private val userIdentifierManager: UserIdentifierManager
) {
    suspend fun execute(
        email: String,
        requestContext: RequestContext
    ): AppResult<SendConfirmation> {
        val rateLimiterEnforcerResult = rateLimiterEnforcer.enforce(
            requestContext = requestContext,
            rateLimitAction = RateLimitAction.SEND_OTP_EMAIL,
            rateLimitIdentifier = email,
            auditAction = AUDIT_ACTION,
            auditResource = AUDIT_RESOURCE,
            auditResourceId = email
        )
        if (rateLimiterEnforcerResult is AppResult.Error) {
            return rateLimiterEnforcerResult
        }

        val identifierResult = userIdentifierManager.getUserIdentifier(
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = email
        )

        val userIdentifier = when (identifierResult) {
            is AppResult.Success -> identifierResult.data
            is AppResult.Error -> return identifierResult
        }

        return if (userIdentifier != null) {
            sendConfirmationCode(email, requestContext)
        } else {
            fakeResetPasswordFlow(email, requestContext)
        }
    }

    private suspend fun sendConfirmationCode(
        email: String,
        requestContext: RequestContext
    ): AppResult<SendConfirmation> {
        val getOtpResult = otpService.getOtp(
            identifier = email,
            type = OtpVerificationType.EMAIL_PASSWORD_RESET
        )

        val code = when (getOtpResult) {
            is AppResult.Success -> getOtpResult.data
            is AppResult.Error -> return getOtpResult
        }

        val sendEmailResult = emailService.sendResetPasswordVerificationCode(email, code)

        if (sendEmailResult is AppResult.Error) {
            return sendEmailResult
        }

        userAuditLogger.logSuccess(
            requestContext = requestContext,
            action = AUDIT_ACTION,
            resource = AUDIT_RESOURCE,
            resourceId = email,
            type = UserAuditMetadata.Types.VERIFICATION_CODE_SENT
        )

        return AppResult.Success(
            SendConfirmation(
                retryAfterSeconds = RETRY_AFTER_SECONDS
            )
        )
    }

    private suspend fun fakeResetPasswordFlow(
        email: String,
        requestContext: RequestContext
    ): AppResult<SendConfirmation> {
        val getOtpResult = otpService.getOtpFake(
            identifier = email
        )

        if (getOtpResult is AppResult.Error) {
            return getOtpResult
        }

        val sendEmailResult = emailService.fakeSendEmail()

        if (sendEmailResult is AppResult.Error) {
            return sendEmailResult
        }

        userAuditLogger.logSuccess(
            requestContext = requestContext,
            action = AUDIT_ACTION,
            resource = AUDIT_RESOURCE,
            resourceId = email,
            type = UserAuditMetadata.Types.EMAIL_NOT_REGISTERED
        )

        return AppResult.Success(
            SendConfirmation(
                retryAfterSeconds = RETRY_AFTER_SECONDS
            )
        )
    }

    companion object {
        const val RETRY_AFTER_SECONDS = 60

        const val AUDIT_ACTION = "send_password_reset_confirmation"
        const val AUDIT_RESOURCE = "user_email"
    }
}