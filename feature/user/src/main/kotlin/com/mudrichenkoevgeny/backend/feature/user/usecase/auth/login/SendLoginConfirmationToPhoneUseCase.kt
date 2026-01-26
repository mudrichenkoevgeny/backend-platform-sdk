package com.mudrichenkoevgeny.backend.feature.user.usecase.auth.login

import com.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import com.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitAction
import com.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import com.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import com.mudrichenkoevgeny.backend.feature.user.enums.OtpVerificationType
import com.mudrichenkoevgeny.backend.feature.user.model.confirmation.SendConfirmation
import com.mudrichenkoevgeny.backend.feature.user.service.otp.OtpService
import com.mudrichenkoevgeny.backend.feature.user.service.phone.PhoneService
import com.mudrichenkoevgeny.backend.feature.user.util.IdentifierMaskerUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendLoginConfirmationToPhoneUseCase @Inject constructor(
    private val rateLimiterEnforcer: RateLimitEnforcer,
    private val userAuditLogger: UserAuditLogger,
    private val otpService: OtpService,
    private val phoneService: PhoneService
) {
    suspend fun execute(
        phoneNumber: String,
        requestContext: RequestContext
    ): AppResult<SendConfirmation> {
        val auditResourceId = IdentifierMaskerUtil.maskPhone(phoneNumber)

        val rateLimiterEnforcerResult = rateLimiterEnforcer.enforce(
            requestContext = requestContext,
            rateLimitAction = RateLimitAction.SEND_OTP_PHONE,
            rateLimitIdentifier = phoneNumber,
            auditAction = AUDIT_ACTION,
            auditResource = AUDIT_RESOURCE,
            auditResourceId = auditResourceId
        )
        if (rateLimiterEnforcerResult is AppResult.Error) {
            return rateLimiterEnforcerResult
        }

        val getOtpResult = otpService.getOtp(
            identifier = phoneNumber,
            type = OtpVerificationType.PHONE_VERIFICATION
        )

        val code = when (getOtpResult) {
            is AppResult.Success -> getOtpResult.data
            is AppResult.Error -> {
                logAuditInternalError(
                    requestContext = requestContext,
                    auditResourceId = auditResourceId
                )
                return getOtpResult
            }
        }

        val sendCodeResult = phoneService.sendVerificationCode(phoneNumber, code)

        if (sendCodeResult is AppResult.Error) {
            logAuditInternalError(
                requestContext = requestContext,
                auditResourceId = auditResourceId
            )
            return sendCodeResult
        }

        userAuditLogger.logSuccess(
            requestContext = requestContext,
            action = AUDIT_ACTION,
            resource = AUDIT_RESOURCE,
            resourceId = auditResourceId,
            type = UserAuditMetadata.Types.VERIFICATION_CODE_SENT
        )

        return AppResult.Success(
            SendConfirmation(
                retryAfterSeconds = RETRY_AFTER_SECONDS
            )
        )
    }

    private fun logAuditInternalError(
        requestContext: RequestContext,
        auditResourceId: String?
    ) {
        userAuditLogger.logInternalError(
            requestContext = requestContext,
            action = AUDIT_ACTION,
            resource = AUDIT_RESOURCE,
            resourceId = auditResourceId
        )
    }

    companion object {
        const val RETRY_AFTER_SECONDS = 60

        const val AUDIT_ACTION = "send_phone_confirmation"
        const val AUDIT_RESOURCE = "user_phone"
    }
}