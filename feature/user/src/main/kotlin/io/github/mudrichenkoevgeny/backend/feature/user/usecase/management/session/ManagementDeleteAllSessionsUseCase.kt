package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.session

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
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.metadata.UserAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.SessionPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class ManagementDeleteAllSessionsUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val userManager: UserManager,
    private val sessionManager: SessionManager,
    private val webSocketManager: WebSocketManager,
    private val authenticationChallengeService: AuthenticationChallengeService
) {
    /**
     * Administratively deletes all active sessions for a specific user and notifies each client via WebSocket.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE] for the management caller.
     *
     * **Security:**
     * - Requires an active management session (STAFF or ADMIN) with appropriate session deletion permissions.
     * - Enforces MFA step-up verification for the management caller via [authenticationChallengeService].
     * - Validates authority level hierarchy: the management caller must have a strictly higher
     *   [UserDetails.authorityLevel] than the target user.
     * - Prevents bulk session deletion for the caller's own account via management routes.
     * - Protects against abuse via [UserManagementRateLimitAction.MANAGEMENT_SESSION_DELETE_ALL].
     *
     * **Workflow:**
     * 1. Validates the management caller's status, identity (no self-targeting), and rate limits.
     * 2. Verifies the target user exists and checks the authority level hierarchy.
     * 3. Determines the required [SessionPermissionCode] based on the target user's role.
     * 4. Ensures the management caller's session is confirmed via MFA step-up.
     * 5. Retrieves the list of current sessions via [sessionManager] to enable WebSocket notifications.
     * 6. Deletes all sessions for the target [userId] via [sessionManager].
     * 7. Sends a [UserWebSocketEventTypes.SESSION_DELETED] frame to each deleted session via [webSocketManager].
     * 8. Logs the action via [AuditLogger] with [UserAuditActionType.MANAGEMENT_DELETE_ALL_USER_SESSIONS].
     *
     * @param userId The ID of the user whose sessions should be deleted.
     * @param authenticatedRequestContext The context of the authenticated management request.
     * @return [AppResult] indicating success or the specific [AppError].
     */
    suspend operator fun invoke(
        userId: UserId,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<Unit> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
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
                baseMetadata = auditMetadata
            )
        }

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserManagementRateLimitAction.MANAGEMENT_SESSION_DELETE_ALL,
            identifier = managementUserId.asHexDashString()
        )
        if (rateLimitCheck is AppResult.Error) {
            return handleError(
                error = rateLimitCheck.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
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
                baseMetadata = auditMetadata
            )
        }

        if (managementUser.accountStatus != UserAccountStatus.ACTIVE) {
            return handleError(
                error = UserError.UserIllegalAccountStatus(managementUserId),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
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
                baseMetadata = auditMetadata
            )
        }

        if (targetUser.authorityLevel >= managementUser.authorityLevel) {
            return handleError(
                error = UserError.UserInsufficientAuthorityLevel(managementUserId),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val requiredPermission = when (targetUser.role) {
            UserRole.USER -> SessionPermissionCode.SESSION_DELETE_FOR_USER
            UserRole.STAFF -> SessionPermissionCode.SESSION_DELETE_FOR_STAFF
            UserRole.ADMIN -> return handleError(
                error = UserError.UserForbidden(managementUserId),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        if (requiredPermission !in managementUser.permissionCodes) {
            return handleError(
                error = UserError.UserMissingPermissions(managementUserId),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
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
                baseMetadata = auditMetadata
            )
        }

        val sessionsToDelete = sessionManager.getAllUserSessions(
            userId = userId
        ).dataOrNull() ?: emptyList()

        val deleteResult = sessionManager.deleteAllUserSessions(userId)

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
                    metadata = auditMetadata
                )
                deleteResult
            }
            is AppResult.Error -> handleError(
                error = deleteResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }
    }

    private fun <T> handleError(
        error: AppError,
        actorId: String,
        actorUserRole: UserRole,
        baseMetadata: Set<AuditEventMetadata>
    ): AppResult<T> {
        val auditErrorLogData = auditErrorConverter.convert(error)
        logAudit(
            actorId = actorId,
            actorUserRole = actorUserRole,
            status = auditErrorLogData.status,
            metadata = baseMetadata + auditErrorLogData.metadata
        )
        return AppResult.Error(error)
    }

    private fun logAudit(
        actorId: String,
        actorUserRole: UserRole,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>
    ) {
        auditLogger.log(
            actorId = actorId,
            actorType = AuditActorType.USER,
            actorUserRole = actorUserRole.serialName,
            action = UserAuditActionType.MANAGEMENT_DELETE_ALL_USER_SESSIONS,
            resource = UserAuditResourceType.SESSION,
            resourceId = null,
            status = status,
            metadata = metadata
        )
    }
}