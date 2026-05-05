package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.dataOrNull
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserManagementRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.metadata.UserAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.UserPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class ManagementDeleteUserUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val userManager: UserManager,
    private val sessionManager: SessionManager,
    private val webSocketManager: WebSocketManager,
    private val authenticationChallengeService: AuthenticationChallengeService
) {
    /**
     * Permanently deletes a user account and terminates all associated active sessions.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE] for the management caller.
     *
     * **Security:**
     * - Requires an active management session (STAFF or ADMIN) with specific deletion permissions.
     * - Enforces MFA step-up verification for the **caller** via [authenticationChallengeService].
     * - Validates authority level hierarchy: the **caller** must have a strictly higher
     *   [UserDetails.authorityLevel] than the target user.
     * - Prevents self-deletion via management routes.
     * - Protects against abuse via [UserManagementRateLimitAction.MANAGEMENT_USER_DELETE].
     *
     * **Workflow:**
     * 1. Validates administrator status, rate limits, and target user existence.
     * 2. Checks authority levels and determines the required [UserPermissionCode] based on target role.
     * 3. Ensures the management session is confirmed (MFA step-up).
     * 4. Identifies all active sessions for the target user via [sessionManager].
     * 5. Deletes the user record via [userManager].
     * 6. Sends [UserWebSocketEventTypes.SESSION_DELETED] frames to all target sessions.
     * 7. Logs the deletion event via [AuditLogger] with [UserAuditActionType.MANAGEMENT_DELETE_USER].
     *
     * @param userId The ID of the user to be deleted.
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

        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata().toMutableSet()
        auditMetadata.add(
            AuditEventMetadata(
                key = UserAuditMetadataKey.USER_ID,
                value = userId.asHexDashString()
            )
        )

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
            action = UserManagementRateLimitAction.MANAGEMENT_USER_DELETE,
            identifier = managementUserId.asHexDashString()
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
            UserRole.USER -> UserPermissionCode.USER_DELETE_FOR_USER
            UserRole.STAFF -> UserPermissionCode.USER_DELETE_FOR_STAFF
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

        val sessionsToDelete = sessionManager.getAllUserSessions(
            userId = userId
        ).dataOrNull() ?: emptyList()

        val deleteResult = userManager.deleteUserForManagement(userId)

        return when (deleteResult) {
            is AppResult.Success -> {
                sessionsToDelete.forEach { session ->
                    webSocketManager.sendMessageToUserSession(
                        userSessionId = session.id,
                        frame = SocketFrame(
                            type = UserWebSocketEventTypes.SESSION_DELETED,
                            timestamp = Clock.System.now().toEpochMilliseconds()
                        )
                    )
                }
                logAudit(
                    status = AuditStatus.SUCCESS,
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    metadata = auditMetadata
                )
                deleteResult
            }
            is AppResult.Error -> handleError(
                error = deleteResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }
    }

    private fun <T> handleError(
        error: AppError,
        actorId: String,
        actorUserRole: UserRole,
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
        actorUserRole: UserRole,
        resourceId: String,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>
    ) {
        auditLogger.log(
            actorId = actorId,
            actorType = AuditActorType.USER,
            actorUserRole = actorUserRole.serialName,
            action = UserAuditActionType.MANAGEMENT_DELETE_USER,
            resource = UserAuditResourceType.USER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}