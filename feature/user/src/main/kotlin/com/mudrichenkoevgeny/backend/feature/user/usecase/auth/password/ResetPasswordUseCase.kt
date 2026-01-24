package com.mudrichenkoevgeny.backend.feature.user.usecase.auth.password

import com.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import com.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import com.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.PasswordPolicyChecker
import com.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.result.PasswordPolicyCheckResult
import com.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitAction
import com.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import com.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import com.mudrichenkoevgeny.backend.feature.user.enums.OtpVerificationType
import com.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import com.mudrichenkoevgeny.backend.feature.user.error.helper.convertToPasswordTooWeak
import com.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import com.mudrichenkoevgeny.backend.feature.user.manager.useridentifier.UserIdentifierManager
import com.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import com.mudrichenkoevgeny.backend.feature.user.service.otp.OtpService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResetPasswordUseCase @Inject constructor(
    private val rateLimiterEnforcer: RateLimitEnforcer,
    private val userAuditLogger: UserAuditLogger,
    private val otpService: OtpService,
    private val passwordPolicyChecker: PasswordPolicyChecker,
    private val userIdentifierManager: UserIdentifierManager
) {
    suspend fun execute(
        email: String,
        newPassword: String,
        confirmationCode: String,
        requestContext: RequestContext
    ): AppResult<UserIdentifier> {
        val auditResourceId = requestContext.userId?.asString()

        val rateLimiterEnforcerResult = rateLimiterEnforcer.enforce(
            requestContext = requestContext,
            rateLimitAction = RateLimitAction.PASSWORD_CHANGE,
            rateLimitIdentifier = email,
            auditAction = AUDIT_ACTION,
            auditResource = AUDIT_RESOURCE,
            auditResourceId = auditResourceId
        )
        if (rateLimiterEnforcerResult is AppResult.Error) {
            return rateLimiterEnforcerResult
        }

        val passwordPolicyCheckResult = passwordPolicyChecker.check(newPassword)

        if (passwordPolicyCheckResult is PasswordPolicyCheckResult.Fail) {
            userAuditLogger.logFail(
                requestContext = requestContext,
                action = AUDIT_ACTION,
                resource = AUDIT_RESOURCE,
                resourceId = auditResourceId,
                type = UserAuditMetadata.Types.TOO_WEAK_PASSWORD
            )

            return AppResult.Error(passwordPolicyCheckResult.convertToPasswordTooWeak())
        }

        val verifyOtpResult = otpService.verifyOtp(
            identifier = email,
            type = OtpVerificationType.EMAIL_PASSWORD_RESET,
            code = confirmationCode
        )

        val isConfirmationCodeCorrect = when (verifyOtpResult) {
            is AppResult.Success -> verifyOtpResult.data
            is AppResult.Error -> return verifyOtpResult
        }

        if (!isConfirmationCodeCorrect) {
            userAuditLogger.logFail(
                requestContext = requestContext,
                action = AUDIT_ACTION,
                resource = AUDIT_RESOURCE,
                resourceId = auditResourceId,
                type = UserAuditMetadata.Types.WRONG_VERIFICATION_CODE
            )
            return AppResult.Error(UserError.WrongConfirmationCode())
        }

        val identifierResult = userIdentifierManager.getUserIdentifier(
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = email
        ).mapNotNullOrError(
            UserError.UserNotFound()
        )

        val userIdentifier = when (identifierResult) {
            is AppResult.Success -> identifierResult.data
            is AppResult.Error -> {
                logAuditInternalError(requestContext, auditResourceId)
                return identifierResult
            }
        }

        val updatedUserIdentifierResult = userIdentifierManager.updateUserIdentifierPassword(
            userIdentifier = userIdentifier,
            identifier = email,
            password = newPassword
        )

        when (updatedUserIdentifierResult) {
            is AppResult.Success -> {
                userAuditLogger.logSuccess(
                    requestContext = requestContext,
                    action = AUDIT_ACTION,
                    resource = AUDIT_RESOURCE,
                    resourceId = auditResourceId
                )
            }
            is AppResult.Error -> {
                logAuditInternalError(requestContext, auditResourceId)
            }
        }

        return updatedUserIdentifierResult
    }

    private fun logAuditInternalError(requestContext: RequestContext, auditResourceId: String?) {
        userAuditLogger.logInternalError(
            requestContext = requestContext,
            action = AUDIT_ACTION,
            resource = AUDIT_RESOURCE,
            resourceId = auditResourceId
        )
    }

    companion object {
        const val AUDIT_ACTION = "password_reset"
        const val AUDIT_RESOURCE = "user"
    }
}