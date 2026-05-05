package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.login

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier.ExternalAuthVerifier
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.metadata.UserAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.data.AuthData
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginByExternalAuthProviderUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val externalAuthVerifiers: Set<@JvmSuppressWildcards ExternalAuthVerifier>,
    private val authSettingsProvider: AuthSettingsProvider,
    private val authManager: AuthManager
) {
    /**
     * Authenticates or creates a user account using an external OAuth2/OIDC provider.
     *
     * **Allowed Account Statuses:** Any (Public access).
     *
     * **Security:**
     * - Validates the [token] against the specified [authProvider] using a collection of [externalAuthVerifiers].
     * - Checks if the provider is currently enabled via [AuthSettingsProvider].
     * - Protects against token-based brute-force or flooding via [UserRateLimitAction.LOGIN_ATTEMPT].
     * - If multifactor authentication (MFA) is enabled for the account, the [authManager] will return
     *   an error directing the user to complete the TOTP challenge.
     *
     * **Workflow:**
     * 1. Verifies rate limits for the provided authentication token.
     * 2. Confirms the [authProvider] is supported and retrieves the corresponding [ExternalAuthVerifier].
     * 3. Validates the external token and retrieves verification data (external ID, email).
     * 4. Delegates authentication or user creation to [authManager].
     * 5. Logs the security event via [AuditLogger] with [UserAuditActionType.LOGIN_BY_EXTERNAL_AUTH_PROVIDER].
     *
     * @param authProvider The external identity provider (e.g., Google, Apple).
     * @param token The identity token provided by the external provider.
     * @param requestContext The context of the public request.
     * @return [AppResult] containing [AuthData] or an MFA challenge.
     */
    suspend operator fun invoke(
        authProvider: UserAuthProvider,
        token: String,
        requestContext: RequestContext
    ): AppResult<AuthData> {
        val auditMetadata = requestContext.clientInfo.toAuditMetadata().toMutableSet()

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.LOGIN_ATTEMPT,
            identifier = token
        )
        if (rateLimitCheck is AppResult.Error) {
            return handleError(
                error = rateLimitCheck.error,
                baseMetadata = auditMetadata
            )
        }

        val externalAuthVerifier = externalAuthVerifiers.find { it.provider == authProvider }
        val authSettings = authSettingsProvider.getPublicAuthSettings()
        val supportedExternalProviders = authSettings.availableAuthProviders.supportedExternalProviders
        val isSupportedAuthProvider = supportedExternalProviders.any { it.userAuthProvider == authProvider }

        if (!isSupportedAuthProvider || externalAuthVerifier == null) {
            return handleError(
                error = UserError.CannotCreateUserIdentifier(),
                baseMetadata = auditMetadata
            )
        }

        val verificationResult = externalAuthVerifier.verify(token)
        val verificationData = when (verificationResult) {
            is AppResult.Error -> return handleError(
                error = verificationResult.error,
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> verificationResult.data
        }

        auditMetadata.add(
            AuditEventMetadata(
                key = UserAuditMetadataKey.EXTERNAL_ID,
                value = verificationData.externalId
            )
        )
        verificationData.email?.let {
            auditMetadata.add(
                AuditEventMetadata(
                    key = UserAuditMetadataKey.EMAIL_ADDRESS,
                    value = it
                )
            )
        }

        val authenticateUserResult = authManager.authenticateOrCreateUser(
            clientInfo = requestContext.clientInfo,
            userAuthProvider = verificationData.authProvider,
            identifier = verificationData.externalId,
            externalProviderEmail = verificationData.email
        )

        return when (authenticateUserResult) {
            is AppResult.Success -> {
                val userDetails = authenticateUserResult.data.userDetails
                logAudit(
                    status = AuditStatus.SUCCESS,
                    actorId = userDetails.id.asHexDashString(),
                    actorUserRole = userDetails.role,
                    resourceId = userDetails.id.asHexDashString(),
                    metadata = auditMetadata
                )
                authenticateUserResult
            }
            is AppResult.Error -> handleError(
                error = authenticateUserResult.error,
                baseMetadata = auditMetadata
            )
        }
    }

    private fun <T> handleError(
        error: AppError,
        actorId: String? = null,
        actorUserRole: UserRole? = null,
        resourceId: String? = null,
        baseMetadata: Set<AuditEventMetadata>
    ): AppResult<T> {
        val auditErrorLogData = auditErrorConverter.convert(error)
        logAudit(
            actorId = actorId,
            actorUserRole = actorUserRole,
            resourceId = resourceId,
            status = auditErrorLogData.status,
            metadata = baseMetadata + auditErrorLogData.metadata
        )
        return AppResult.Error(error)
    }

    private fun logAudit(
        actorId: String? = null,
        actorUserRole: UserRole? = null,
        resourceId: String? = null,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>
    ) {
        auditLogger.log(
            actorId = actorId,
            actorType = AuditActorType.USER,
            actorUserRole = actorUserRole?.serialName,
            action = UserAuditActionType.LOGIN_BY_EXTERNAL_AUTH_PROVIDER,
            resource = UserAuditResourceType.USER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}