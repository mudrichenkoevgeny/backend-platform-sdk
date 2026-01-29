package io.github.mudrichenkoevgeny.backend.feature.user.usecase.auth.register

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.enums.OtpVerificationType
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import io.github.mudrichenkoevgeny.backend.feature.user.manager.useridentifier.UserIdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.confirmation.SendConfirmation
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.EmailService
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.feature.user.util.IdentifierMaskerUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendRegistrationConfirmationToEmailUseCase @Inject constructor(
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
        val auditResourceId = IdentifierMaskerUtil.maskEmail(email)

        val rateLimiterEnforcerResult = rateLimiterEnforcer.enforce(
            requestContext = requestContext,
            rateLimitAction = RateLimitAction.SEND_OTP_EMAIL,
            rateLimitIdentifier = email,
            auditAction = AUDIT_ACTION,
            auditResource = AUDIT_RESOURCE,
            auditResourceId = auditResourceId
        )
        if (rateLimiterEnforcerResult is AppResult.Error) {
            return rateLimiterEnforcerResult
        }

        val identifierResult = userIdentifierManager.getUserIdentifier(
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = email
        )

        val identifier = when (identifierResult) {
            is AppResult.Success -> identifierResult.data
            is AppResult.Error -> {
                logAuditInternalError(
                    requestContext = requestContext,
                    auditResourceId = auditResourceId
                )
                return identifierResult
            }
        }

        return if (identifier != null) {
            sendAlreadyRegistered(email, requestContext, auditResourceId)
        } else {
            sendConfirmationCode(email, requestContext, auditResourceId)
        }
    }

    private suspend fun sendAlreadyRegistered(
        email: String,
        requestContext: RequestContext,
        auditResourceId: String?
    ): AppResult<SendConfirmation> {
        val getOtpResult = otpService.getOtpFake(
            identifier = email
        )

        if (getOtpResult is AppResult.Error) {
            logAuditInternalError(
                requestContext = requestContext,
                auditResourceId = auditResourceId
            )
            return getOtpResult
        }

        val sendEmailResult = emailService.sendAlreadyRegisteredEmail(
            email = email,
            ipAddress = requestContext.clientInfo.ipAddress,
            deviceName = requestContext.clientInfo.deviceName
        )

        if (sendEmailResult is AppResult.Error) {
            logAuditInternalError(
                requestContext = requestContext,
                auditResourceId = auditResourceId
            )
            return sendEmailResult
        }

        userAuditLogger.logSuccess(
            requestContext = requestContext,
            action = AUDIT_ACTION,
            resource = AUDIT_RESOURCE,
            resourceId = auditResourceId,
            type = UserAuditMetadata.Types.ALREADY_REGISTERED
        )

        return AppResult.Success(
            SendConfirmation(
                retryAfterSeconds = RETRY_AFTER_SECONDS
            )
        )
    }

    private suspend fun sendConfirmationCode(
        email: String,
        requestContext: RequestContext,
        auditResourceId: String?
    ): AppResult<SendConfirmation> {
        val getOtpResult = otpService.getOtp(
            identifier = email,
            type = OtpVerificationType.EMAIL_VERIFICATION
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

        val sendEmailResult = emailService.sendVerificationCode(email, code)

        if (sendEmailResult is AppResult.Error) {
            logAuditInternalError(
                requestContext = requestContext,
                auditResourceId = auditResourceId
            )
            return sendEmailResult
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

        const val AUDIT_ACTION = "send_email_confirmation"
        const val AUDIT_RESOURCE = "user_email"
    }
}