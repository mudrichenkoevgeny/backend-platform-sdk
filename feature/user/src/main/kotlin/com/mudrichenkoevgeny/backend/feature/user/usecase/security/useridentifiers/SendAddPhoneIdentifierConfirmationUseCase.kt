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
import com.mudrichenkoevgeny.backend.feature.user.service.otp.OtpService
import com.mudrichenkoevgeny.backend.feature.user.service.phone.PhoneService
import com.mudrichenkoevgeny.backend.feature.user.util.IdentifierMaskerUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendAddPhoneIdentifierConfirmationUseCase @Inject constructor(
    private val rateLimiterEnforcer: RateLimitEnforcer,
    private val userAuditLogger: UserAuditLogger,
    private val otpService: OtpService,
    private val phoneService: PhoneService,
    private val sessionManager: SessionManager,
    private val userIdentifierManager: UserIdentifierManager,
    private val authenticationPolicyChecker: AuthenticationPolicyChecker
) {
    suspend fun execute(
        phoneNumber: String,
        requestContext: RequestContext
    ): AppResult<SendConfirmation> {
        val userId = requestContext.userId
            ?: return AppResult.Error(UserError.InvalidAccessToken())

        val currentSessionId = requestContext.sessionId
            ?: return AppResult.Error(UserError.InvalidSession())

        val auditResourceId = userId.asString()

        val auditMetadata = mutableMapOf(
            UserAuditMetadata.Keys.PHONE_NUMBER_MASK to IdentifierMaskerUtil.maskPhone(phoneNumber),
            UserAuditMetadata.Keys.SESSION_ID to currentSessionId.asString()
        )

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
            userAuthProvider = UserAuthProvider.PHONE,
            identifier = phoneNumber
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
            sendAlreadyRegistered(phoneNumber, requestContext, auditResourceId, auditMetadata)
        } else {
            sendConfirmationCode(phoneNumber, requestContext, auditResourceId, auditMetadata)
        }
    }

    private suspend fun sendAlreadyRegistered(
        phoneNumber: String,
        requestContext: RequestContext,
        auditResourceId: String,
        auditMetadata: MutableMap<String, String>
    ): AppResult<SendConfirmation> {
        val getOtpResult = otpService.getOtpFake(
            identifier = phoneNumber
        )

        if (getOtpResult is AppResult.Error) {
            logAuditInternalError(
                requestContext = requestContext,
                auditResourceId = auditResourceId,
                auditMetadata = auditMetadata
            )
            return getOtpResult
        }

        val sendAlreadyRegisteredResult = phoneService.sendAlreadyRegisteredPhoneNumber(
            phoneNumber = phoneNumber,
            ipAddress = requestContext.clientInfo.ipAddress,
            deviceName = requestContext.clientInfo.deviceName
        )

        if (sendAlreadyRegisteredResult is AppResult.Error) {
            logAuditInternalError(
                requestContext = requestContext,
                auditResourceId = auditResourceId,
                auditMetadata = auditMetadata
            )
            return sendAlreadyRegisteredResult
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
        phoneNumber: String,
        requestContext: RequestContext,
        auditResourceId: String,
        auditMetadata: MutableMap<String, String>
    ): AppResult<SendConfirmation> {
        val getOtpResult = otpService.getOtp(
            identifier = phoneNumber,
            type = OtpVerificationType.PHONE_VERIFICATION
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

        val sendCodeResult = phoneService.sendVerificationCode(phoneNumber, code)

        if (sendCodeResult is AppResult.Error) {
            logAuditInternalError(
                requestContext = requestContext,
                auditResourceId = auditResourceId,
                auditMetadata = auditMetadata
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

        const val AUDIT_ACTION = "send_add_phone_identifier_confirmation"
        const val AUDIT_RESOURCE = "user"
    }
}