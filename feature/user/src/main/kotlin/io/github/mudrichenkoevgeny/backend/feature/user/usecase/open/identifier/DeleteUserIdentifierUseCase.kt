package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.metadata.UserAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class DeleteUserIdentifierUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val userManager: UserManager,
    private val sessionManager: SessionManager,
    private val identifierManager: IdentifierManager,
    private val webSocketManager: WebSocketManager,
    private val authenticationChallengeService: AuthenticationChallengeService
) {
    /**
     * Deletes a specific authentication identifier belonging to the current user.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY].
     *
     * **Security:**
     * - Sensitive operation requiring MFA Step-up (session confirmation).
     * - Prevents deletion if it's the user's last remaining identifier.
     *
     * **Workflow:**
     * 1. Checks rate limits for [UserRateLimitAction.USER_IDENTIFIER_DELETE].
     * 2. Ensures the current session is recently confirmed via [AuthenticationChallengeService].
     * 3. Verifies that the user has at least one other identifier remaining.
     * 4. Deletes the identifier via [IdentifierManager].
     * 5. Terminates all active sessions associated with the deleted identifier and notifies them via [WebSocketManager].
     * 6. Logs the security event via [AuditLogger] with [UserAuditActionType.SELF_DELETE_IDENTIFIER].
     *
     * @param userIdentifierId The identifier of the record to be deleted.
     * @param authenticatedRequestContext The context of the authenticated request.
     * @return [AppResult] indicating successful deletion.
     */
    suspend operator fun invoke(
        userIdentifierId: UserIdentifierId,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<Unit> {
        val currentUserId = authenticatedRequestContext.userId
        val currentSessionId = authenticatedRequestContext.sessionId

        val auditActorId = currentUserId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditResourceId = userIdentifierId.asHexDashString()

        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata().toMutableSet()
        auditMetadata.add(
            AuditEventMetadata(
                key = UserAuditMetadataKey.SESSION_ID,
                value = currentSessionId.asHexDashString()
            )
        )

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.USER_IDENTIFIER_DELETE,
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

        val userResult = userManager.getUserByIdForSelf(currentUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val currentUser = when (userResult) {
            is AppResult.Error -> return handleError(
                error = userResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
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
                resourceId = auditResourceId,
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
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        val userIdentifiersListResult = identifierManager.getUserIdentifiersByUserId(currentUserId)
        val userIdentifiersList = when (userIdentifiersListResult) {
            is AppResult.Success -> userIdentifiersListResult.data
            is AppResult.Error -> return handleError(
                error = userIdentifiersListResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        val identifierToDelete = userIdentifiersList.find { it.id == userIdentifierId }

        if (identifierToDelete == null || userIdentifiersList.size < 2) {
            return handleError(
                error = UserError.CannotDeleteUserIdentifier(),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        val userSessionsResult = sessionManager.getAllUserSessions(currentUserId)
        val userSessions = when (userSessionsResult) {
            is AppResult.Success -> userSessionsResult.data
            is AppResult.Error -> return handleError(
                error = userSessionsResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        val deleteUserIdentifierResult = identifierManager.deleteUserIdentifier(
            userIdentifierId = userIdentifierId
        )

        when (deleteUserIdentifierResult) {
            is AppResult.Error -> {
                return handleError(
                    error = deleteUserIdentifierResult.error,
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    baseMetadata = auditMetadata
                )
            }
            is AppResult.Success -> {
                val sessionsToDelete = userSessions.filter { it.identifierId == userIdentifierId }
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
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    status = AuditStatus.SUCCESS,
                    metadata = auditMetadata
                )
            }
        }

        return deleteUserIdentifierResult
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
            action = UserAuditActionType.SELF_DELETE_IDENTIFIER,
            resource = UserAuditResourceType.IDENTIFIER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}