package com.mudrichenkoevgeny.backend.feature.user.usecase.security.useridentifiers

import com.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import com.mudrichenkoevgeny.backend.core.security.authenticationpolicychecker.AuthenticationPolicyChecker
import com.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitAction
import com.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import com.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import com.mudrichenkoevgeny.backend.feature.user.enums.OtpVerificationType
import com.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import com.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import com.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import com.mudrichenkoevgeny.backend.feature.user.manager.useridentifier.UserIdentifierManager
import com.mudrichenkoevgeny.backend.feature.user.model.confirmation.SendConfirmation
import com.mudrichenkoevgeny.backend.feature.user.service.email.EmailService
import com.mudrichenkoevgeny.backend.feature.user.service.otp.OtpService
import com.mudrichenkoevgeny.backend.feature.user.util.IdentifierMaskerUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendAddEmailIdentifierConfirmationUseCase @Inject constructor(
    private val rateLimiterEnforcer: RateLimitEnforcer,
    private val userAuditLogger: UserAuditLogger,
    private val otpService: OtpService,
    private val emailService: EmailService,
    private val sessionManager: SessionManager,
    private val userIdentifierManager: UserIdentifierManager,
    private val authenticationPolicyChecker: AuthenticationPolicyChecker
) {
    suspend fun execute(
        email: String,
        requestContext: RequestContext
    ): AppResult<SendConfirmation> {
        val userId = requestContext.userId
            ?: return AppResult.Error(UserError.InvalidAccessToken())

        val currentSessionId = requestContext.sessionId
            ?: return AppResult.Error(UserError.InvalidSession())

        val auditResourceId = userId.asString()

        val auditMetadata = mutableMapOf(
            UserAuditMetadata.Keys.EMAIL_MASK to IdentifierMaskerUtil.maskEmail(email),
            UserAuditMetadata.Keys.SESSION_ID to currentSessionId.asString()
        )

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

        val userSessionResult = sessionManager.getUserSessionById(currentSessionId)

        val currentSession = when (userSessionResult) {
            is AppResult.Success -> userSessionResult.data
            is AppResult.Error -> {
                logAuditInternalError(
                    requestContext = requestContext,
                    auditResourceId = auditResourceId,
                    auditMetadata = auditMetadata
                )
                return userSessionResult
            }
        }

        if (currentSession == null) {
            logAuditInternalError(
                requestContext = requestContext,
                auditResourceId = auditResourceId,
                auditMetadata = auditMetadata
            )
            return AppResult.Error(UserError.CannotCreateUserIdentifier())
        }

        val isAuthenticationConfirmedRecently = authenticationPolicyChecker.isAuthenticationConfirmedRecently(
            lastReauthenticatedAt = currentSession.lastReauthenticatedAt
        )

        if (!isAuthenticationConfirmedRecently) {
            userAuditLogger.logFail(
                requestContext = requestContext,
                action = AUDIT_ACTION,
                resource = AUDIT_RESOURCE,
                resourceId = auditResourceId,
                type = UserAuditMetadata.Types.AUTHENTICATION_CONFIRMATION_REQUIRED,
                metadata = auditMetadata
            )
            return AppResult.Error(UserError.AuthenticationConfirmationRequired())
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
                    auditResourceId = auditResourceId,
                    auditMetadata = auditMetadata
                )
                return identifierResult
            }
        }

        return if (identifier != null) {
            sendAlreadyRegistered(email, requestContext, auditResourceId, auditMetadata)
        } else {
            sendConfirmationCode(email, requestContext, auditResourceId, auditMetadata)
        }
    }

    private suspend fun sendAlreadyRegistered(
        email: String,
        requestContext: RequestContext,
        auditResourceId: String,
        auditMetadata: MutableMap<String, String>
    ): AppResult<SendConfirmation> {
        val getOtpResult = otpService.getOtpFake(
            identifier = email
        )

        if (getOtpResult is AppResult.Error) {
            logAuditInternalError(
                requestContext = requestContext,
                auditResourceId = auditResourceId,
                auditMetadata = auditMetadata
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
                auditResourceId = auditResourceId,
                auditMetadata = auditMetadata
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
        auditResourceId: String,
        auditMetadata: MutableMap<String, String>
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
                    auditResourceId = auditResourceId,
                    auditMetadata = auditMetadata
                )
                return getOtpResult
            }
        }

        val sendEmailResult = emailService.sendVerificationCode(email, code)

        if (sendEmailResult is AppResult.Error) {
            logAuditInternalError(
                requestContext = requestContext,
                auditResourceId = auditResourceId,
                auditMetadata = auditMetadata
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
        auditResourceId: String?,
        auditMetadata: Map<String, String>
    ) {
        userAuditLogger.logInternalError(
            requestContext = requestContext,
            action = AUDIT_ACTION,
            resource = AUDIT_RESOURCE,
            resourceId = auditResourceId,
            metadata = auditMetadata
        )
    }

    companion object {
        const val RETRY_AFTER_SECONDS = 60

        const val AUDIT_ACTION = "send_add_email_identifier_confirmation"
        const val AUDIT_RESOURCE = "user"
    }
}