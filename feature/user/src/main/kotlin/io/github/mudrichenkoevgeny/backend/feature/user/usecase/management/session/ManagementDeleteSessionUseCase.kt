package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.session

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
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
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSession
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.SessionPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class ManagementDeleteSessionUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val userManager: UserManager,
    private val sessionManager: SessionManager,
    private val webSocketManager: WebSocketManager,
    private val authenticationChallengeService: AuthenticationChallengeService
) {
    /**
     * Administratively revokes a specific user session and notifies the client via WebSocket.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE] for the management caller.
     *
     * **Security:**
     * - Requires an active management session (STAFF or ADMIN) with specific session deletion permissions.
     * - Enforces MFA step-up verification for the management caller via [authenticationChallengeService].
     * - Validates authority level hierarchy: the management caller must have a strictly higher
     *   [UserDetails.authorityLevel] than the session owner.
     * - Prevents self-session revocation via management routes.
     * - Protects against abuse via [UserManagementRateLimitAction.MANAGEMENT_SESSION_DELETE].
     *
     * **Workflow:**
     * 1. Validates the management caller's status and rate limits.
     * 2. Retrieves the target [UserSession] and its owner to verify hierarchy and permissions.
     * 3. Determines required [SessionPermissionCode] based on the target user's role.
     * 4. Ensures the management caller's session is confirmed via MFA step-up.
     * 5. Deletes the session via [sessionManager].
     * 6. Sends a [UserWebSocketEventTypes.SESSION_DELETED] frame to the deleted session via [webSocketManager].
     * 7. Logs the action via [AuditLogger] with [UserAuditActionType.MANAGEMENT_DELETE_SESSION].
     *
     * @param userSessionId The ID of the session to be deleted.
     * @param authenticatedRequestContext The context of the authenticated management request.
     * @return [AppResult] indicating success or the specific [AppError].
     */
    suspend operator fun invoke(
        userSessionId: UserSessionId,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<Unit> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditResourceId = userSessionId.asHexDashString()
        val managementUserId = authenticatedRequestContext.userId

        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata().toMutableSet()

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserManagementRateLimitAction.MANAGEMENT_SESSION_DELETE,
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

        val userSessionResult = sessionManager.getUserSessionForSystem(userSessionId)
            .mapNotNullOrError(
                CommonError.NotFound(
                    resource = UserSession::class.java.simpleName,
                    identifier = userSessionId.asHexDashString()
                )
            )

        val userSession = when (userSessionResult) {
            is AppResult.Success -> userSessionResult.data
            is AppResult.Error -> return handleError(
                error = userSessionResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        val targetUserResult = userManager.getUserByIdForSelf(userSession.userId)
            .mapNotNullOrError(UserError.UserNotFound(userSession.userId))

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

        auditMetadata.add(
            AuditEventMetadata(
                key = UserAuditMetadataKey.USER_ID,
                value = targetUser.id.asHexDashString()
            )
        )

        if (managementUserId == targetUser.id) {
            return handleError(
                error = UserError.UserForbidden(managementUserId),
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
            UserRole.USER -> SessionPermissionCode.SESSION_DELETE_FOR_USER
            UserRole.STAFF -> SessionPermissionCode.SESSION_DELETE_FOR_STAFF
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

        val deleteResult = sessionManager.deleteSessionById(userSessionId)

        return when (deleteResult) {
            is AppResult.Success -> {
                webSocketManager.sendMessageToUserSession(
                    userSessionId = userSessionId,
                    frame = SocketFrame(
                        type = UserWebSocketEventTypes.SESSION_DELETED,
                        timestamp = Clock.System.now().toEpochMilliseconds()
                    )
                )
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
            action = UserAuditActionType.MANAGEMENT_DELETE_SESSION,
            resource = UserAuditResourceType.SESSION,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}