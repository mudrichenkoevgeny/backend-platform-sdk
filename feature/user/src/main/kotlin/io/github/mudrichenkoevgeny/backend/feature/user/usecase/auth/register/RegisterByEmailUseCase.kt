package io.github.mudrichenkoevgeny.backend.feature.user.usecase.auth.register

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.model.RateLimitAction
import io.github.mudrichenkoevgeny.backend.core.security.settings.usecase.ValidatePasswordUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.model.otp.OtpVerificationType
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthData
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.feature.user.util.IdentifierMaskerUtil
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserRole
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case: register a new user by email with password and email verification code.
 *
 * Applies rate limiting, validates password policy, verifies OTP, then creates the email identifier and provides auth data via [AuthManager].
 * [execute] takes email, password, confirmationCode, and request context;
 * returns [AppResult.Success] with [AuthData] or [AppResult.Error] (e.g. [UserError.WrongConfirmationCode], weak password, rate limit).
 */
@Singleton
class RegisterByEmailUseCase @Inject constructor(
    private val rateLimiterEnforcer: RateLimitEnforcer,
    private val userAuditLogger: UserAuditLogger,
    private val otpService: OtpService,
    private val authManager: AuthManager,
    private val validatePasswordUseCase: ValidatePasswordUseCase
) {
    suspend fun execute(
        email: String,
        password: String,
        confirmationCode: String,
        requestContext: RequestContext
    ): AppResult<AuthData> {
        val auditResourceId = requestContext.userId?.asHexDashString()
        val auditMetadata = mapOf(UserAuditMetadata.Keys.EMAIL_MASK to IdentifierMaskerUtil.maskEmail(email))

        val rateLimiterEnforcerResult = rateLimiterEnforcer.enforce(
            requestContext = requestContext,
            rateLimitAction = RateLimitAction.REGISTRATION_ATTEMPT,
            rateLimitIdentifier = email,
            auditAction = AUDIT_ACTION,
            auditResource = AUDIT_RESOURCE,
            auditResourceId = auditResourceId
        )
        if (rateLimiterEnforcerResult is AppResult.Error) {
            return rateLimiterEnforcerResult
        }

        val passwordPolicyCheckResult = validatePasswordUseCase(password)

        if (passwordPolicyCheckResult is AppResult.Error) {
            userAuditLogger.logFail(
                requestContext = requestContext,
                action = AUDIT_ACTION,
                resource = AUDIT_RESOURCE,
                resourceId = auditResourceId,
                type = UserAuditMetadata.Types.TOO_WEAK_PASSWORD,
                metadata = auditMetadata
            )
            return passwordPolicyCheckResult
        }

        val verifyOtpResult = otpService.verifyOtp(
            identifier = email,
            type = OtpVerificationType.EMAIL_VERIFICATION,
            code = confirmationCode
        )

        val isConfirmationCodeCorrect = when (verifyOtpResult) {
            is AppResult.Success -> verifyOtpResult.data
            is AppResult.Error -> {
                logAuditInternalError(
                    requestContext = requestContext,
                    auditResourceId = auditResourceId,
                    auditMetadata = auditMetadata
                )
                return verifyOtpResult
            }
        }

        if (!isConfirmationCodeCorrect) {
            userAuditLogger.logFail(
                requestContext = requestContext,
                action = AUDIT_ACTION,
                resource = AUDIT_RESOURCE,
                resourceId = auditResourceId,
                type = UserAuditMetadata.Types.WRONG_VERIFICATION_CODE,
                metadata = auditMetadata
            )
            return AppResult.Error(UserError.WrongConfirmationCode())
        }

        val userIdentifierResult = authManager.getOrCreateUserIdentifier(
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = email,
            password = password,
            userRole = UserRole.USER
        )

        val userIdentifier = when (userIdentifierResult) {
            is AppResult.Success -> userIdentifierResult.data
            is AppResult.Error -> {
                logAuditInternalError(
                    requestContext = requestContext,
                    auditResourceId = auditResourceId,
                    auditMetadata = auditMetadata
                )
                return userIdentifierResult
            }
        }

        val authDataResult = authManager.provideAuthData(
            userIdentifier = userIdentifier,
            clientInfo = requestContext.clientInfo,
            allowedRoles = setOf(UserRole.USER)
        )

        when (authDataResult) {
            is AppResult.Success -> {
                userAuditLogger.logSuccess(
                    requestContext = requestContext,
                    action = AUDIT_ACTION,
                    resource = AUDIT_RESOURCE,
                    resourceId = auditResourceId,
                    metadata = auditMetadata
                )
            }
            is AppResult.Error -> {
                logAuditInternalError(
                    requestContext = requestContext,
                    auditResourceId = auditResourceId,
                    auditMetadata = auditMetadata
                )
            }
        }

        return authDataResult
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
        const val AUDIT_ACTION = "register_by_email"
        const val AUDIT_RESOURCE = "user"
    }
}