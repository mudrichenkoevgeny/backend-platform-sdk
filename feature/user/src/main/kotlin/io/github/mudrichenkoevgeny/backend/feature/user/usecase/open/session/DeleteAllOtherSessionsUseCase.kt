package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.session

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
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
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.DeletedSessions
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class DeleteAllOtherSessionsUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val sessionManager: SessionManager,
    private val webSocketManager: WebSocketManager
) {
    /**
     * Terminates all active sessions for the current user, excluding the one initiating the request.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY].
     *
     * **Workflow:**
     * 1. Checks rate limits for [UserRateLimitAction.SESSION_DELETE].
     * 2. Identifies and removes all session records except the current one via [SessionManager].
     * 3. Sends a [UserWebSocketEventTypes.SESSION_DELETED] frame via [WebSocketManager] to each terminated session.
     * 4. Logs the bulk deletion via [AuditLogger] with [UserAuditActionType.SELF_DELETE_OTHER_SESSIONS].
     *
     * @param authenticatedRequestContext The context of the authenticated request.
     * @return [AppResult] containing [DeletedSessions] with the identifiers of terminated sessions.
     */
    suspend operator fun invoke(
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<DeletedSessions> {
        val currentSessionId = authenticatedRequestContext.sessionId

        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditResourceId = currentSessionId.asHexDashString()
        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata().toMutableSet()
        auditMetadata.add(
            AuditEventMetadata(
                key = UserAuditMetadataKey.SESSION_ID,
                value = currentSessionId.asHexDashString()
            )
        )

        val currentUserId = authenticatedRequestContext.userId

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.SESSION_DELETE,
            identifier = currentUserId.asHexDashString()
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

        val deleteSessionsResult = sessionManager.deleteAllSessionsExceptOneForSelf(
            userId = currentUserId,
            userSessionId = currentSessionId
        )

        when (deleteSessionsResult) {
            is AppResult.Success -> {
                deleteSessionsResult.data.deletedSessionIds.forEach { deletedUserSessionId ->
                    webSocketManager.sendMessageToUserSession(
                        userSessionId = deletedUserSessionId,
                        frame = SocketFrame(
                            type = UserWebSocketEventTypes.SESSION_DELETED,
                            timestamp = Clock.System.now().toEpochMilliseconds()
                        )
                    )
                }
                logAudit(
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    status = AuditStatus.SUCCESS,
                    metadata = auditMetadata
                )
            }
            is AppResult.Error -> {
                return handleError(
                    error = deleteSessionsResult.error,
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    baseMetadata = auditMetadata
                )
            }
        }

        return deleteSessionsResult
    }

    private fun <T> handleError(
        error: AppError,
        actorId: String? = null,
        actorUserRole: UserRole? = null,
        resourceId: String?,
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
        actorUserRole: UserRole?,
        resourceId: String?,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>
    ) {
        auditLogger.log(
            actorId = actorId,
            actorType = AuditActorType.USER,
            actorUserRole = actorUserRole?.serialName,
            action = UserAuditActionType.SELF_DELETE_OTHER_SESSIONS,
            resource = UserAuditResourceType.SESSION,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}