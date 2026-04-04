package io.github.mudrichenkoevgeny.backend.feature.user.usecase.auth.password

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.model.RateLimitAction
import io.github.mudrichenkoevgeny.backend.core.security.usecase.open.passwordpolicy.ValidatePasswordUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.model.otp.OtpVerificationType
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.useridentifier.UserIdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.OtpService
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.audit.resource.UserAuditResourceType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case: reset a user's password by email using a verification code.
 *
 * Applies rate limiting, validates new password policy, verifies OTP, then updates the email identifier's password via [UserIdentifierManager].
 * [execute] takes email, newPassword, confirmationCode, and request context;
 * returns [AppResult.Success] with updated [UserIdentifier] or [AppResult.Error] (e.g. [UserError.WrongConfirmationCode], [UserError.UserNotFound]).
 */
@Singleton
class ResetPasswordUseCase @Inject constructor(
    private val rateLimiterEnforcer: RateLimitEnforcer,
    private val userAuditLogger: UserAuditLogger,
    private val otpService: OtpService,
    private val userIdentifierManager: UserIdentifierManager,
    private val validatePasswordUseCase: ValidatePasswordUseCase
) {
    suspend fun execute(
        email: String,
        newPassword: String,
        confirmationCode: String,
        requestContext: RequestContext
    ): AppResult<UserIdentifier> {
        val auditResourceId = requestContext.userId?.asHexDashString()

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

        val passwordPolicyCheckResult = validatePasswordUseCase(newPassword)

        if (passwordPolicyCheckResult is AppResult.Error) {
            userAuditLogger.logFail(
                requestContext = requestContext,
                action = AUDIT_ACTION,
                resource = AUDIT_RESOURCE,
                resourceId = auditResourceId,
                type = UserAuditMetadata.Types.TOO_WEAK_PASSWORD
            )
            return passwordPolicyCheckResult
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
        const val AUDIT_ACTION = UserAuditActionType.ACTION_RESET_PASSWORD
        const val AUDIT_RESOURCE = UserAuditResourceType.RESOURCE_USER
    }
}