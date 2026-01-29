package io.github.mudrichenkoevgeny.backend.feature.user.usecase.auth.login

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.enums.OtpVerificationType
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserRole
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthData
import io.github.mudrichenkoevgeny.backend.feature.user.service.otp.OtpService
import io.github.mudrichenkoevgeny.backend.feature.user.util.IdentifierMaskerUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginByPhoneUseCase @Inject constructor(
    private val rateLimiterEnforcer: RateLimitEnforcer,
    private val userAuditLogger: UserAuditLogger,
    private val otpService: OtpService,
    private val authManager: AuthManager
) {
    suspend fun execute(
        phoneNumber: String,
        confirmationCode: String,
        requestContext: RequestContext
    ): AppResult<AuthData> {
        val auditResourceId = requestContext.userId?.asString()
        val auditMetadata = mapOf(
            UserAuditMetadata.Keys.PHONE_NUMBER_MASK to IdentifierMaskerUtil.maskPhone(phoneNumber)
        )

        val rateLimiterEnforcerResult = rateLimiterEnforcer.enforce(
            requestContext = requestContext,
            rateLimitAction = RateLimitAction.LOGIN_ATTEMPT,
            rateLimitIdentifier = phoneNumber,
            auditAction = AUDIT_ACTION,
            auditResource = AUDIT_RESOURCE,
            auditResourceId = auditResourceId
        )
        if (rateLimiterEnforcerResult is AppResult.Error) {
            return rateLimiterEnforcerResult
        }

        val verifyOtpResult = otpService.verifyOtp(
            identifier = phoneNumber,
            type = OtpVerificationType.PHONE_VERIFICATION,
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
            userAuthProvider = UserAuthProvider.PHONE,
            identifier = phoneNumber,
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
        const val AUDIT_ACTION = "login_by_phone"
        const val AUDIT_RESOURCE = "user"
    }
}