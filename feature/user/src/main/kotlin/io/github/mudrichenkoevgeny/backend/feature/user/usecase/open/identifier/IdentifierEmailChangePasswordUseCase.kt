package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.core.security.passwordhasher.PasswordHasher
import io.github.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import javax.inject.Inject
import javax.inject.Singleton

// todo refactor
/**
 * Use case: change the current user's password (authenticated, email identifier).
 *
 * Applies rate limiting, validates new password policy, verifies old password via [io.github.mudrichenkoevgeny.backend.core.security.passwordhasher.PasswordHasher], then updates the identifier password via [io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager].
 * [execute] takes email, newPassword, oldPassword, and request context;
 * returns [io.github.mudrichenkoevgeny.backend.core.common.result.AppResult.Success] with updated [io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier] or [io.github.mudrichenkoevgeny.backend.core.common.result.AppResult.Error] (e.g. [io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError.WrongPassword], [io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError.UserNotFound]).
 */
@Singleton
class IdentifierEmailChangePasswordUseCase @Inject constructor(
    private val rateLimiterEnforcer: RateLimitEnforcer,
    private val userAuditLogger: UserAuditLogger,
    private val passwordHasher: PasswordHasher,
    private val identifierManager: IdentifierManager,
    private val validatePasswordUseCase: ValidatePasswordUseCase
) {
    suspend fun execute(
        email: String,
        newPassword: String,
        oldPassword: String,
        requestContext: RequestContext
    ): AppResult<UserIdentifier> {
        val auditResourceId = requestContext.userId?.asHexDashString()

        val rateLimiterEnforcerResult = rateLimiterEnforcer.enforce(
            requestContext = requestContext,
            rateLimitAction = UserRateLimitAction.PASSWORD_CHANGE,
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

        val identifierResult = identifierManager.getUserIdentifier(
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

        val updatedUserIdentifierResult = identifierManager.updateUserIdentifierPassword(
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
        const val AUDIT_ACTION = UserAuditActionType.ACTION_CHANGE_PASSWORD
        const val AUDIT_RESOURCE = UserAuditResourceType.RESOURCE_USER
    }
}