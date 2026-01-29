package io.github.mudrichenkoevgeny.backend.feature.user.usecase.auth.login

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginByExternalAuthProviderUseCase @Inject constructor(
    private val rateLimiterEnforcer: RateLimitEnforcer,
    private val userAuditLogger: UserAuditLogger
) {
    suspend fun execute(
        authProviderKey: String,
        token: String,
        requestContext: RequestContext
    ): AppResult<AuthData> {
        val auditResourceId = requestContext.userId?.asString()
        val auditMetadata = mutableMapOf(UserAuditMetadata.Keys.EXTERNAL_AUTH_PROVIDER to authProviderKey)

        val rateLimiterEnforcerResult = rateLimiterEnforcer.enforce(
            requestContext = requestContext,
            rateLimitAction = RateLimitAction.LOGIN_ATTEMPT,
            rateLimitIdentifier = token,
            auditAction = AUDIT_ACTION,
            auditResource = AUDIT_RESOURCE,
            auditResourceId = auditResourceId
        )
        if (rateLimiterEnforcerResult is AppResult.Error) {
            return rateLimiterEnforcerResult
        }

        val authProvider = UserAuthProvider.fromValue(authProviderKey)
        val supportedExternalProviders: Set<UserAuthProvider> = emptySet()

        if (authProvider == null || !supportedExternalProviders.contains(authProvider)) {
            userAuditLogger.logFail(
                requestContext = requestContext,
                action = AUDIT_ACTION,
                resource = AUDIT_RESOURCE,
                resourceId = auditResourceId,
                type = UserAuditMetadata.Types.NOT_SUPPORTED_EXTERNAL_AUTH_PROVIDER,
                metadata = auditMetadata
            )
            return AppResult.Error(UserError.ExternalIdMismatch())
        }

        // todo not implemented
        return AppResult.Error(UserError.ExternalIdMismatch())
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
        const val AUDIT_ACTION = "login_by_external_auth_provider"
        const val AUDIT_RESOURCE = "user"
    }
}