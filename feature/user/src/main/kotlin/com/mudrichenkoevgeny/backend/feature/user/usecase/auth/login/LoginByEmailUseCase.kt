package io.github.mudrichenkoevgeny.backend.feature.user.usecase.auth.login

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserRole
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.useridentifier.UserIdentifierManager
import io.github.mudrichenkoevgeny.backend.core.security.passwordhasher.PasswordHasher
import io.github.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthData
import io.github.mudrichenkoevgeny.backend.feature.user.util.IdentifierMaskerUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginByEmailUseCase @Inject constructor(
    private val rateLimiterEnforcer: RateLimitEnforcer,
    private val userAuditLogger: UserAuditLogger,
    private val passwordHasher: PasswordHasher,
    private val userIdentifierManager: UserIdentifierManager,
    private val authManager: AuthManager
) {
    suspend fun execute(
        email: String,
        password: String,
        requestContext: RequestContext
    ): AppResult<AuthData> {
        val auditResourceId = requestContext.userId?.asString()
        val auditMetadata = mapOf(UserAuditMetadata.Keys.EMAIL_MASK to IdentifierMaskerUtil.maskEmail(email))

        val rateLimiterEnforcerResult = rateLimiterEnforcer.enforce(
            requestContext = requestContext,
            rateLimitAction = RateLimitAction.LOGIN_ATTEMPT,
            rateLimitIdentifier = email,
            auditAction = AUDIT_ACTION,
            auditResource = AUDIT_RESOURCE,
            auditResourceId = auditResourceId
        )
        if (rateLimiterEnforcerResult is AppResult.Error) {
            return rateLimiterEnforcerResult
        }

        val userIdentifierResult = userIdentifierManager.getUserIdentifier(
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = email
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

        if (userIdentifier == null) {
            passwordHasher.isPasswordValidFakeCheck(password)
            userAuditLogger.logFail(
                requestContext = requestContext,
                action = AUDIT_ACTION,
                resource = AUDIT_RESOURCE,
                resourceId = auditResourceId,
                type = UserAuditMetadata.Types.EMAIL_NOT_REGISTERED
            )
            return AppResult.Error(UserError.InvalidCredentials())
        }

        val isPasswordValidResult = passwordHasher.isPasswordValid(password, userIdentifier.passwordHash)

        val isPasswordValid = when (isPasswordValidResult) {
            is AppResult.Success -> isPasswordValidResult.data
            is AppResult.Error -> {
                logAuditInternalError(
                    requestContext = requestContext,
                    auditResourceId = auditResourceId,
                    auditMetadata = auditMetadata
                )
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
            return AppResult.Error(UserError.InvalidCredentials())
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
                userAuditLogger.logInternalError(
                    requestContext = requestContext,
                    action = AUDIT_ACTION,
                    resource = AUDIT_RESOURCE,
                    resourceId = auditResourceId,
                    metadata = auditMetadata
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
        const val AUDIT_ACTION = "login_by_email"
        const val AUDIT_RESOURCE = "user"
    }
}