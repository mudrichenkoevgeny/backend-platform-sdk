package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.unlock

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier.ExternalAuthVerifier
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
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
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnlockByExternalAuthProviderUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val externalAuthVerifiers: Set<@JvmSuppressWildcards ExternalAuthVerifier>,
    private val authSettingsProvider: AuthSettingsProvider,
    private val securitySettingsProvider: SecuritySettingsProvider,
    private val identifierManager: IdentifierManager,
    private val userManager: UserManager
) {
    /**
     * Unlocks a temporarily locked user account using an external OAuth2/OIDC provider token.
     *
     * **Allowed Account Statuses:** Any (Self-service unlock flow).
     *
     * **Security:**
     * - Checks if self-service unlock is enabled via [SecuritySettingsProvider].
     * - Enforces rate limits on unlock attempts.
     * - Verifies the external token via [ExternalAuthVerifier].
     *
     * **Workflow:**
     * 1. Confirms self-service unlock is enabled in account lockout policy.
     * 2. Enforces rate limits for the provided token.
     * 3. Verifies token via corresponding [ExternalAuthVerifier].
     * 4. Resolves the external identifier via [IdentifierManager].
     * 5. Unlocks the user account via [UserManager.unlockUserAccount].
     * 6. Logs the security audit event via [AuditLogger] with [UserAuditActionType.SELF_UNLOCK_ACCOUNT].
     *
     * @param authProvider External identity provider (e.g. Google, Apple).
     * @param token Opaque token issued by the external provider.
     * @param requestContext Request context containing client details.
     * @return [AppResult.Success] with [Unit] upon successful unlock, or an error.
     */
    suspend operator fun invoke(
        authProvider: UserAuthProvider,
        token: String,
        requestContext: RequestContext
    ): AppResult<Unit> {
        val auditMetadata = requestContext.clientInfo.toAuditMetadata().toMutableSet()
        auditMetadata.add(
            AuditEventMetadata(
                key = UserAuditMetadataKey.USER_AUTH_PROVIDER,
                value = authProvider.serialName
            )
        )

        val accountLockoutPolicy = securitySettingsProvider.getAccountLockoutPolicy()
        if (!accountLockoutPolicy.isSelfServiceUnlockEnabled) {
            return handleError(
                error = UserError.SelfServiceUnlockDisabled(),
                baseMetadata = auditMetadata
            )
        }

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
        val authSettings = authSettingsProvider.getOpenAuthSettings()
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

        val identifierResult = identifierManager.getUserIdentifierInternalByProvider(
            userAuthProvider = authProvider,
            identifier = verificationData.externalId
        ).mapNotNullOrError(UserError.UserNotFound())

        val userIdentifier = when (identifierResult) {
            is AppResult.Success -> identifierResult.data
            is AppResult.Error -> return handleError(
                error = identifierResult.error,
                baseMetadata = auditMetadata
            )
        }

        val unlockResult = userManager.unlockUserAccount(userIdentifier.userId)
        return when (unlockResult) {
            is AppResult.Error -> handleError(
                error = unlockResult.error,
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> {
                logAudit(
                    status = AuditStatus.SUCCESS,
                    actorId = userIdentifier.userId.asHexDashString(),
                    resourceId = userIdentifier.userId.asHexDashString(),
                    metadata = auditMetadata
                )
                AppResult.Success(Unit)
            }
        }
    }

    private fun <T> handleError(
        error: AppError,
        actorId: String? = null,
        resourceId: String? = null,
        baseMetadata: Set<AuditEventMetadata>
    ): AppResult<T> {
        val auditErrorLogData = auditErrorConverter.convert(error)
        logAudit(
            actorId = actorId,
            resourceId = resourceId,
            status = auditErrorLogData.status,
            metadata = baseMetadata + auditErrorLogData.metadata
        )
        return AppResult.Error(error)
    }

    private fun logAudit(
        actorId: String? = null,
        resourceId: String? = null,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>
    ) {
        auditLogger.log(
            actorId = actorId,
            actorType = AuditActorType.USER,
            action = UserAuditActionType.SELF_UNLOCK_ACCOUNT,
            resource = UserAuditResourceType.USER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}
