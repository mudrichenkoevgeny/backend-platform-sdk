package io.github.mudrichenkoevgeny.backend.feature.user.usecase.security.password

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.core.security.passwordhasher.PasswordHasher
import io.github.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.PasswordPolicyChecker
import io.github.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.result.PasswordPolicyCheckResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import io.github.mudrichenkoevgeny.backend.feature.user.error.helper.convertToPasswordTooWeak
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.useridentifier.UserIdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordChangeUseCase @Inject constructor(
    private val rateLimiterEnforcer: RateLimitEnforcer,
    private val userAuditLogger: UserAuditLogger,
    private val passwordHasher: PasswordHasher,
    private val passwordPolicyChecker: PasswordPolicyChecker,
    private val userIdentifierManager: UserIdentifierManager
) {
    suspend fun execute(
        email: String,
        newPassword: String,
        oldPassword: String,
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

        val isPasswordValidResult = passwordHasher.isPasswordValid(oldPassword, userIdentifier.passwordHash)

        val isPasswordValid = when (isPasswordValidResult) {
            is AppResult.Success -> isPasswordValidResult.data
            is AppResult.Error -> {
                logAuditInternalError(requestContext, auditResourceId)
                return isPasswordValidResult
            }
        }

        if (!isPasswordValid) {
            userAuditLogger.logFail(
                requestContext = requestContext,
                action = AUDIT_ACTION,
                resource = AUDIT_RESOURCE,
                resourceId = auditResourceId,
                type = UserAuditMetadata.Types.WRONG_PASSWORD
            )
            return AppResult.Error(UserError.WrongPassword())
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
        const val AUDIT_ACTION = "password_change"
        const val AUDIT_RESOURCE = "user"
    }
}