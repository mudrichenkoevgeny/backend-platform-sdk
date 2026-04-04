package io.github.mudrichenkoevgeny.backend.feature.user.usecase.auth.login

import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.RequestContext
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter.RateLimitEnforcer
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.model.RateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.audit.UserAuditMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier.ExternalAuthVerifier
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthData
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.ExternalAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case: authenticate a user via an external provider token (e.g. Google).
 *
 * Resolves the provider from the given key, checks it is supported via [AuthSettingsProvider],
 * verifies the token with the matching [ExternalAuthVerifier], then gets or creates the identifier and provides auth data via [AuthManager].
 * [execute] takes authProviderKey, token, and request context;
 * returns [AppResult.Success] with [AuthData] or [AppResult.Error] (e.g. [UserError.ExternalIdMismatch], unsupported provider).
 */
@Singleton
class LoginByExternalAuthProviderUseCase @Inject constructor(
    private val rateLimiterEnforcer: RateLimitEnforcer,
    private val userAuditLogger: UserAuditLogger,
    private val externalAuthVerifiers: Set<@JvmSuppressWildcards ExternalAuthVerifier>,
    private val authSettingsProvider: AuthSettingsProvider,
    private val authManager: AuthManager
) {
    suspend fun execute(
        authProviderKey: String,
        token: String,
        requestContext: RequestContext
    ): AppResult<AuthData> {
        val auditResourceId = requestContext.userId?.asHexDashString()
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
        val externalAuthVerifier = externalAuthVerifiers.find { it.provider == authProvider }

        val authSettingsResult = authSettingsProvider.getSettings()
        if (authSettingsResult is AppResult.Error) {
            return AppResult.Error(authSettingsResult.error)
        }

        val supportedExternalProviders: Set<ExternalAuthProvider> = (authSettingsResult as AppResult.Success).data
            .availableAuthProviders.supportedExternalProviders

        val isSupportedAuthProvider = supportedExternalProviders.any { it.userAuthProvider == authProvider }

        if (authProvider == null || !isSupportedAuthProvider || externalAuthVerifier == null) {
            logAuditFail(
                requestContext = requestContext,
                auditResourceId = auditResourceId,
                type = UserAuditMetadata.Types.NOT_SUPPORTED_EXTERNAL_AUTH_PROVIDER,
                auditMetadata = auditMetadata
            )
            return AppResult.Error(UserError.ExternalIdMismatch())
        }

        val verificationResult = externalAuthVerifier.verify(token)

        if (verificationResult is AppResult.Error) {
            logAuditFail(
                requestContext = requestContext,
                auditResourceId = auditResourceId,
                type = UserAuditMetadata.Types.EXTERNAL_ID_MISMATCH,
                auditMetadata = auditMetadata
            )
            return verificationResult
        }

        val verificationData = (verificationResult as AppResult.Success).data

        val userIdentifierResult = authManager.getOrCreateUserIdentifier(
            userAuthProvider = verificationData.authProvider,
            identifier = verificationData.externalId,
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

    private fun logAuditFail(
        requestContext: RequestContext,
        auditResourceId: String?,
        auditMetadata: Map<String, String>,
        type: String?
    ) {
        userAuditLogger.logFail(
            requestContext = requestContext,
            action = AUDIT_ACTION,
            resource = AUDIT_RESOURCE,
            resourceId = auditResourceId,
            type = type,
            metadata = auditMetadata
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
        const val AUDIT_ACTION = "login_by_external_auth_provider"
        const val AUDIT_RESOURCE = "user"
    }
}