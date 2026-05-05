package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.security.passwordhasher.PasswordHasher
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.usecase.open.passwordpolicy.ValidatePasswordUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.metadata.UserAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdentifierEmailChangePasswordUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val userManager: UserManager,
    private val sessionManager: SessionManager,
    private val identifierManager: IdentifierManager,
    private val passwordHasher: PasswordHasher,
    private val authenticationChallengeService: AuthenticationChallengeService,
    private val validatePasswordUseCase: ValidatePasswordUseCase
) {
    /**
     * Updates the password for an email-based identifier after validating the current credentials.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY].
     *
     * **Security:**
     * - Sensitive operation requiring MFA Step-up (session confirmation).
     * - Enforces password complexity rules via [ValidatePasswordUseCase].
     *
     * **Workflow:**
     * 1. Checks rate limits for [UserRateLimitAction.PASSWORD_CHANGE].
     * 2. Ensures the current session is recently confirmed via [AuthenticationChallengeService].
     * 3. Validates the [newPassword] against the system's password policy.
     * 4. Verifies the [oldPassword] against the existing hash using [PasswordHasher].
     * 5. Updates the identifier record with the newly hashed password.
     * 6. Logs the security event via [AuditLogger] with [UserAuditActionType.CHANGE_PASSWORD].
     *
     * @param email The email identifier for which the password is being changed.
     * @param newPassword The new password string.
     * @param oldPassword The current password for verification.
     * @param authenticatedRequestContext The context of the authenticated request.
     * @return [AppResult] containing the updated [UserIdentifier].
     */
    suspend operator fun invoke(
        email: String,
        newPassword: String,
        oldPassword: String,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<UserIdentifier> {
        val currentUserId = authenticatedRequestContext.userId
        val currentSessionId = authenticatedRequestContext.sessionId

        val auditActorId = currentUserId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata().toMutableSet()

        auditMetadata.add(
            AuditEventMetadata(
                key = UserAuditMetadataKey.SESSION_ID,
                value = currentSessionId.asHexDashString()
            )
        )

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.PASSWORD_CHANGE,
            identifier = email
        )
        if (rateLimitCheck is AppResult.Error) {
            return handleError(
                error = rateLimitCheck.error,
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
            userSessionId = currentSessionId
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

        val passwordPolicyCheckResult = validatePasswordUseCase(newPassword)
        if (passwordPolicyCheckResult is AppResult.Error) {
            return handleError(
                error = passwordPolicyCheckResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val identifierResult = identifierManager.getUserIdentifierInternalByProvider(
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = email
        ).mapNotNullOrError(UserError.UserNotFound())

        val userIdentifier = when (identifierResult) {
            is AppResult.Error -> return handleError(
                error = identifierResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> identifierResult.data
        }

        val auditResourceId = userIdentifier.id.asHexDashString()

        val isPasswordValidResult = passwordHasher.isPasswordValid(oldPassword, userIdentifier.passwordHash)
        when (isPasswordValidResult) {
            is AppResult.Error -> return handleError(
                error = isPasswordValidResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
            is AppResult.Success -> if (!isPasswordValidResult.data) {
                return handleError(
                    error = UserError.WrongPassword(),
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    baseMetadata = auditMetadata
                )
            }
        }

        val updateResult = identifierManager.updateUserIdentifierPassword(
            userIdentifier = userIdentifier,
            password = newPassword
        )

        when (updateResult) {
            is AppResult.Error -> {
                return handleError(
                    error = updateResult.error,
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    baseMetadata = auditMetadata
                )
            }
            is AppResult.Success -> {
                logAudit(
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    status = AuditStatus.SUCCESS,
                    metadata = auditMetadata
                )
            }
        }

        return updateResult
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
            action = UserAuditActionType.CHANGE_PASSWORD,
            resource = UserAuditResourceType.IDENTIFIER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}