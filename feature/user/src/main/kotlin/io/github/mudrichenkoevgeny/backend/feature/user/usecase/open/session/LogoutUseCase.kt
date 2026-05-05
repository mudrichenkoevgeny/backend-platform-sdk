package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.session

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.uuid.Uuid

@Singleton
class LogoutUseCase @Inject constructor(
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val sessionManager: SessionManager,
    private val webSocketManager: WebSocketManager
) {
    /**
     * Terminates the current active session of the authenticated user.
     *
     * **Allowed Account Statuses:** Any.
     *
     * **Workflow:**
     * 1. Extracts the sessionId from the authenticated request context.
     * 2. Deletes the session from persistent storage via [SessionManager].
     * 3. Sends a [UserWebSocketEventTypes.SESSION_DELETED] frame via [WebSocketManager] to notify the client.
     * 4. Logs the logout event via [AuditLogger] with [UserAuditActionType.LOGOUT].
     *
     * @param authenticatedRequestContext The context of the authenticated request.
     * @return [AppResult] indicating successful termination of the session.
     */
    suspend operator fun invoke(
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<Unit> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditResourceId = authenticatedRequestContext.sessionId.asHexDashString()
        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata()

        val currentSessionId = authenticatedRequestContext.sessionId

        val deleteSessionResult = sessionManager.deleteSessionById(currentSessionId)

        if (deleteSessionResult is AppResult.Error) {
            return handleError(
                error = deleteSessionResult.error,
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

        webSocketManager.sendMessageToUserSession(
            userSessionId = currentSessionId,
            frame = SocketFrame(
                id = Uuid.random().toHexDashString(),
                type = UserWebSocketEventTypes.SESSION_DELETED,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
        )

        return deleteSessionResult
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
            action = UserAuditActionType.LOGOUT,
            resource = UserAuditResourceType.SESSION,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}