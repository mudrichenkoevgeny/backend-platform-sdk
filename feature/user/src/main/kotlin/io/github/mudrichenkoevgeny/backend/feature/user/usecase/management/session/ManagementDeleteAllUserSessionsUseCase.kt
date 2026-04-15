package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.session

import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.security.audit.toDeniedSecurityAuditEventMetadata
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.audit.toDeniedUserAuditEventMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.audit.toErrorUserAuditEventMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserManagementRateLimitAction
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.metadata.UserAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.SessionPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class ManagementDeleteAllUserSessionsUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val userManager: UserManager,
    private val sessionManager: SessionManager,
    private val webSocketManager: WebSocketManager
) {
    suspend operator fun invoke(
        userId: UserId,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<Unit> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val managementUserId = authenticatedRequestContext.userId
        val managementSessionId = authenticatedRequestContext.sessionId

        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata() + setOf(
            AuditEventMetadata(
                key = UserAuditMetadataKey.SESSION_ID,
                value = managementSessionId.asHexDashString()
            ),
            AuditEventMetadata(
                key = UserAuditMetadataKey.USER_ID,
                value = userId.asHexDashString()
            )
        )

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserManagementRateLimitAction.MANAGEMENT_SESSION_DELETE_ALL,
            identifier = userId.asHexDashString()
        )

        if (rateLimitCheck is AppResult.Error) {
            logAudit(
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                status = AuditStatus.DENIED,
                metadata = auditMetadata + rateLimitCheck.error.toDeniedSecurityAuditEventMetadata()
            )
            return rateLimitCheck
        }

        val managementUserResult = userManager.getUserById(managementUserId)
            .mapNotNullOrError(UserError.UserForbidden())
        val managementUser = when (managementUserResult) {
            is AppResult.Error -> return managementUserResult
            is AppResult.Success -> managementUserResult.data
        }

        val targetUserResult = userManager.getUserById(userId)
            .mapNotNullOrError(UserError.UserNotFound(userId))
        val targetUser = when (targetUserResult) {
            is AppResult.Error -> return targetUserResult
            is AppResult.Success -> targetUserResult.data
        }

        val requiredPermission = when (targetUser.role) {
            UserRole.USER -> SessionPermissionCode.SESSION_DELETE_FOR_USER
            UserRole.STAFF -> SessionPermissionCode.SESSION_DELETE_FOR_STAFF
            UserRole.ADMIN -> null
        }
        val hasPermission = requiredPermission?.let { it in managementUser.permissions } ?: false
        if (!hasPermission) {
            val error = UserError.UserMissingPermissions(managementUserId)
            logAudit(
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                status = AuditStatus.DENIED,
                metadata = auditMetadata + error.toDeniedUserAuditEventMetadata()
            )
            return AppResult.Error(error)
        }

        val managementSessionResult = sessionManager.getUserSessionById(managementSessionId)
        val managementSession = when (managementSessionResult) {
            is AppResult.Error -> {
                logAudit(
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    status = AuditStatus.FAILED,
                    metadata = auditMetadata + managementSessionResult.error.toErrorUserAuditEventMetadata()
                )
                return managementSessionResult
            }
            is AppResult.Success -> managementSessionResult.data
        }

        if (managementSession == null || managementSession.userId != managementUserId) {
            val error = UserError.UserForbidden(managementUserId)
            logAudit(
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                status = AuditStatus.DENIED,
                metadata = auditMetadata + error.toDeniedUserAuditEventMetadata()
            )
            return AppResult.Error(error)
        }

        /*
        // todo wait for authentication confirmation release
        val isAuthenticationConfirmedRecently = authenticationPolicyChecker.isAuthenticationConfirmedRecentlyForManagement(
            lastReauthenticatedAt = managementSession.lastReauthenticatedAt
        )

        if (!isAuthenticationConfirmedRecently) {
            val error = SecurityError.AuthenticationConfirmationRequired()
            logAudit(
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                status = AuditStatus.DENIED,
                metadata = auditMetadata + error.toDeniedUserAuditEventMetadata()
            )
            return AppResult.Error(error)
        }
        */

        val sessionsResult = sessionManager.getAllUserSessions(userId)
        val sessions = when (sessionsResult) {
            is AppResult.Error -> {
                logAudit(
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    status = AuditStatus.FAILED,
                    metadata = auditMetadata + sessionsResult.error.toErrorUserAuditEventMetadata()
                )
                return sessionsResult
            }
            is AppResult.Success -> sessionsResult.data
        }

        val deleteAllUserSessionsResult = sessionManager.revokeAllUserSessions(userId)
        when (deleteAllUserSessionsResult) {
            is AppResult.Error -> {
                logAudit(
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    status = AuditStatus.FAILED,
                    metadata = auditMetadata + deleteAllUserSessionsResult.error.toErrorUserAuditEventMetadata()
                )
            }
            is AppResult.Success -> {
                sessions.forEach { session ->
                    webSocketManager.sendMessageToUserSession(
                        userSessionId = session.id,
                        frame = SocketFrame(
                            type = UserWebSocketEventTypes.SESSION_TERMINATED,
                            timestamp = Clock.System.now().toEpochMilliseconds()
                        )
                    )
                }
                logAudit(
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    status = AuditStatus.SUCCESS,
                    metadata = auditMetadata
                )
            }
        }

        return deleteAllUserSessionsResult
    }

    private fun logAudit(
        actorId: String? = null,
        actorUserRole: UserRole? = null,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>
    ) {
        auditLogger.log(
            actorId = actorId,
            actorType = AuditActorType.USER,
            actorUserRole = actorUserRole?.serialName,
            action = UserAuditActionType.MANAGEMENT_DELETE_ALL_USER_SESSIONS,
            resource = UserAuditResourceType.USER_SESSION,
            resourceId = null,
            status = status,
            metadata = metadata
        )
    }
}
