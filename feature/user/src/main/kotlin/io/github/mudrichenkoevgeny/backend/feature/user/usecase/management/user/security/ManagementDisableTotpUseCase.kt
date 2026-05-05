package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user.security

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.totp.TotpManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserManagementRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.UserPermissionCode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagementDisableTotpUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val userManager: UserManager,
    private val sessionManager: SessionManager,
    private val totpManager: TotpManager,
    private val authenticationChallengeService: AuthenticationChallengeService
) {
    /**
     * Administratively disables TOTP (2FA) for a specific user.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE] for the management caller.
     *
     * **Security:**
     * - Requires an active management session (STAFF or ADMIN) with appropriate security update permissions.
     * - Enforces MFA step-up verification for the management caller via [authenticationChallengeService].
     * - Validates authority level hierarchy: the management caller's [UserDetails.authorityLevel] must be strictly
     *   greater than the target's.
     * - Prevents self-disabling of TOTP via management routes.
     * - Protects against abuse via [UserManagementRateLimitAction.MANAGEMENT_USER_UPDATE].
     *
     * **Workflow:**
     * 1. Validates the management caller's status, identity (no self-targeting), and rate limits.
     * 2. Verifies the target user exists and currently has TOTP enabled.
     * 3. Checks authority levels and determines the required [UserPermissionCode] based on target role.
     * 4. Ensures the management caller's session is confirmed via MFA step-up.
     * 5. Disables TOTP via [totpManager].
     * 6. Logs the action via [AuditLogger] with [UserAuditActionType.MANAGEMENT_DISABLE_USER_TOTP].
     *
     * @param userId The ID of the target user whose TOTP should be disabled.
     * @param authenticatedRequestContext The context of the authenticated management request.
     * @return [AppResult] indicating success or the specific [AppError].
     */
    suspend operator fun invoke(
        userId: UserId,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<Unit> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditResourceId = userId.asHexDashString()
        val managementUserId = authenticatedRequestContext.userId

        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata()

        if (managementUserId == userId) {
            return handleError(
                error = UserError.UserForbidden(managementUserId),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserManagementRateLimitAction.MANAGEMENT_DISABLE_TOTP,
            identifier = auditActorId
        )
        if (rateLimitCheck is AppResult.Error) {
            return handleError(
                error = rateLimitCheck.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        val managementUserResult = userManager.getUserByIdForSelf(managementUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val managementUser = when (managementUserResult) {
            is AppResult.Success -> managementUserResult.data
            is AppResult.Error -> return handleError(
                error = managementUserResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        if (managementUser.accountStatus != UserAccountStatus.ACTIVE) {
            return handleError(
                error = UserError.UserIllegalAccountStatus(managementUserId),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        val targetUserResult = userManager.getUserByIdForSelf(userId)
            .mapNotNullOrError(UserError.UserNotFound(userId))

        val targetUser = when (targetUserResult) {
            is AppResult.Success -> targetUserResult.data
            is AppResult.Error -> return handleError(
                error = targetUserResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        if (!targetUser.isTotpEnabled) {
            return handleError(
                error = SecurityError.TotpNotEnabled(),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        if (targetUser.authorityLevel >= managementUser.authorityLevel) {
            return handleError(
                error = UserError.UserInsufficientAuthorityLevel(managementUserId),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        val requiredPermission = when (targetUser.role) {
            UserRole.USER -> UserPermissionCode.USER_UPDATE_SECURITY_FOR_USER
            UserRole.STAFF -> UserPermissionCode.USER_UPDATE_SECURITY_FOR_STAFF
            UserRole.ADMIN -> return handleError(
                error = UserError.UserForbidden(managementUserId),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        if (requiredPermission !in managementUser.permissionCodes) {
            return handleError(
                error = UserError.UserMissingPermissions(managementUserId),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        val managementUserSessionResult = sessionManager.getUserSessionForSystem(
            userSessionId = authenticatedRequestContext.sessionId
        ).mapNotNullOrError(UserError.InvalidSession())

        val managementUserSession = when (managementUserSessionResult) {
            is AppResult.Success -> managementUserSessionResult.data
            is AppResult.Error -> return handleError(
                error = managementUserSessionResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        val ensureSessionConfirmedResult = authenticationChallengeService.ensureSessionConfirmed(
            userDetails = managementUser,
            userSession = managementUserSession
        )
        if (ensureSessionConfirmedResult is AppResult.Error) {
            return handleError(
                error = ensureSessionConfirmedResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        val disableResult = totpManager.disableTotp(userId)
        if (disableResult is AppResult.Error) {
            return handleError(
                error = disableResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        logAudit(
            actorId = auditActorId,
            actorUserRole = auditActorUserRole,
            resourceId = auditResourceId,
            status = AuditStatus.SUCCESS,
            metadata = auditMetadata
        )

        return AppResult.Success(Unit)
    }

    private fun <T> handleError(
        error: AppError,
        actorId: String,
        actorUserRole: UserRole?,
        resourceId: String,
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
        actorUserRole: UserRole?,
        resourceId: String,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>
    ) {
        auditLogger.log(
            actorId = actorId,
            actorType = AuditActorType.USER,
            actorUserRole = actorUserRole?.serialName,
            action = UserAuditActionType.MANAGEMENT_DISABLE_USER_TOTP,
            resource = UserAuditResourceType.USER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}