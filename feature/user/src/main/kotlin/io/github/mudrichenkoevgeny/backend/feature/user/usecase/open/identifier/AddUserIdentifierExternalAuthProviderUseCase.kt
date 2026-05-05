package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.auth.verifier.ExternalAuthVerifier
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings.AuthSettingsProvider
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.metadata.UserAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddUserIdentifierExternalAuthProviderUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val userManager: UserManager,
    private val sessionManager: SessionManager,
    private val authManager: AuthManager,
    private val externalAuthVerifiers: Set<@JvmSuppressWildcards ExternalAuthVerifier>,
    private val authSettingsProvider: AuthSettingsProvider,
    private val authenticationChallengeService: AuthenticationChallengeService
) {
    /**
     * Links an external authentication provider (e.g., Google, Apple) to an existing user account.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY].
     *
     * **Security:**
     * - Sensitive operation requiring MFA Step-up (session confirmation).
     * - Verifies external tokens using specialized [ExternalAuthVerifier] implementations.
     * - Checks if the requested provider is enabled in the system via [AuthSettingsProvider].
     *
     * **Workflow:**
     * 1. Checks rate limits for [UserRateLimitAction.USER_IDENTIFIER_ADD].
     * 2. Validates that the provider is supported and a verifier is available.
     * 3. Ensures the current session is recently confirmed via [AuthenticationChallengeService].
     * 4. Verifies the external [token] to retrieve the provider's unique identifier and email.
     * 5. Creates a new external identifier record via [AuthManager].
     * 6. Logs the security event via [AuditLogger] with [UserAuditActionType.ADD_IDENTIFIER_EXTERNAL_AUTH_PROVIDER].
     *
     * @param authProvider The external provider type to be linked.
     * @param token The identity token or authorization code from the external provider.
     * @param authenticatedRequestContext The context of the authenticated request.
     * @return [AppResult] containing the newly created [UserIdentifier].
     */
    suspend operator fun invoke(
        authProvider: UserAuthProvider,
        token: String,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<UserIdentifier> {
        val currentUserId = authenticatedRequestContext.userId
        val currentSessionId = authenticatedRequestContext.sessionId

        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata().toMutableSet()
        auditMetadata.add(
            AuditEventMetadata(
                key = UserAuditMetadataKey.SESSION_ID,
                value = currentSessionId.asHexDashString()
            )
        )

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.USER_IDENTIFIER_ADD,
            identifier = currentUserId.asHexDashString()
        )
        if (rateLimitCheck is AppResult.Error) {
            return handleError(
                error = rateLimitCheck.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val externalAuthVerifier = externalAuthVerifiers.find { it.provider == authProvider }

        val availableAuthProviders = authSettingsProvider.getAvailableAuthProviders()
        val supportedExternalProviders = availableAuthProviders.supportedExternalProviders
        val isSupportedAuthProvider = supportedExternalProviders.any { it.userAuthProvider == authProvider }

        if (!isSupportedAuthProvider || externalAuthVerifier == null) {
            return handleError(
                error = UserError.CannotCreateUserIdentifier(),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val userResult = userManager.getUserByIdForSelf(currentUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val currentUser = when (userResult) {
            is AppResult.Error -> return handleError(
                error = userResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> userResult.data
        }

        val userSessionResult = sessionManager.getUserSessionForSystem(
            userSessionId = authenticatedRequestContext.sessionId
        ).mapNotNullOrError(UserError.InvalidSession())

        val currentSession = when (userSessionResult) {
            is AppResult.Success -> userSessionResult.data
            is AppResult.Error -> return handleError(
                error = userSessionResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val ensureSessionConfirmedResult = authenticationChallengeService.ensureSessionConfirmed(
            userDetails = currentUser,
            userSession = currentSession
        )
        if (ensureSessionConfirmedResult is AppResult.Error) {
            return handleError(
                error = ensureSessionConfirmedResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val verificationResult = externalAuthVerifier.verify(token)
        val verificationData = when (verificationResult) {
            is AppResult.Error -> {
                return handleError(
                    error = verificationResult.error,
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    baseMetadata = auditMetadata
                )
            }
            is AppResult.Success -> verificationResult.data
        }

        auditMetadata.add(
            AuditEventMetadata(
                key = UserAuditMetadataKey.EXTERNAL_ID,
                value = verificationData.externalId
            )
        )

        val createIdentifierForSelfResult = authManager.createIdentifierForAuthorizedUser(
            userId = currentUserId,
            userAuthProvider = verificationData.authProvider,
            identifier = verificationData.externalId,
            password = null,
            externalProviderEmail = verificationData.email
        )
        when (createIdentifierForSelfResult) {
            is AppResult.Error -> {
                return handleError(
                    error = createIdentifierForSelfResult.error,
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    baseMetadata = auditMetadata
                )
            }
            is AppResult.Success -> {
                logAudit(
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = createIdentifierForSelfResult.data.id.asHexDashString(),
                    status = AuditStatus.SUCCESS,
                    metadata = auditMetadata
                )
            }
        }

        return createIdentifierForSelfResult
    }

    private fun <T> handleError(
        error: AppError,
        actorId: String,
        actorUserRole: UserRole,
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
        actorId: String,
        actorUserRole: UserRole,
        resourceId: String? = null,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>
    ) {
        auditLogger.log(
            actorId = actorId,
            actorType = AuditActorType.USER,
            actorUserRole = actorUserRole.serialName,
            action = UserAuditActionType.ADD_IDENTIFIER_EXTERNAL_AUTH_PROVIDER,
            resource = UserAuditResourceType.IDENTIFIER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}