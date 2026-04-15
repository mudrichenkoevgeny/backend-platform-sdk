package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier

import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.dataOrNull
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.security.audit.toDeniedSecurityAuditEventMetadata
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.audit.toDeniedUserAuditEventMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.audit.toErrorUserAuditEventMetadata
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
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
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.IdentifierPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class ManagementDeleteUserIdentifierUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val userManager: UserManager,
    private val sessionManager: SessionManager,
    private val identifierManager: IdentifierManager,
    private val webSocketManager: WebSocketManager
) {
    suspend operator fun invoke(
        userId: UserId,
        userIdentifierId: UserIdentifierId,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<Unit> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditResourceId = userIdentifierId.asHexDashString()
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
            action = UserManagementRateLimitAction.MANAGEMENT_IDENTIFIER_DELETE,
            identifier = userId.asHexDashString()
        )

        if (rateLimitCheck is AppResult.Error) {
            logAudit(
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                status = AuditStatus.DENIED,
                metadata = auditMetadata + rateLimitCheck.error.toDeniedSecurityAuditEventMetadata()
            )
            return rateLimitCheck
        }

        val getManagementUserResult = userManager.getUserById(managementUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val managementUser = when (getManagementUserResult) {
            is AppResult.Error -> return getManagementUserResult
            is AppResult.Success -> getManagementUserResult.data
        }

        val targetUserResult = userManager.getUserById(userId)
            .mapNotNullOrError(UserError.UserNotFound())
        val targetUser = when (targetUserResult) {
            is AppResult.Error -> return targetUserResult
            is AppResult.Success -> targetUserResult.data
        }

        val requiredPermission = when (targetUser.role) {
            UserRole.USER -> IdentifierPermissionCode.IDENTIFIER_DELETE_FOR_USER
            UserRole.STAFF -> IdentifierPermissionCode.IDENTIFIER_DELETE_FOR_STAFF
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

        val userIdentifiersListResult = identifierManager.getUserIdentifiersByUserId(userId)

        val userIdentifiersList = when (userIdentifiersListResult) {
            is AppResult.Success -> userIdentifiersListResult.data
            is AppResult.Error -> {
                logAudit(
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    status = AuditStatus.FAILED,
                    metadata = auditMetadata + userIdentifiersListResult.error.toErrorUserAuditEventMetadata()
                )
                return userIdentifiersListResult
            }
        }

        val managementSessionResult = sessionManager.getUserSessionById(managementSessionId)

        val managementSession = when (managementSessionResult) {
            is AppResult.Success -> managementSessionResult.data
            is AppResult.Error -> {
                logAudit(
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    status = AuditStatus.FAILED,
                    metadata = auditMetadata + managementSessionResult.error.toErrorUserAuditEventMetadata()
                )
                return managementSessionResult
            }
        }

        val identifierIdToDelete = userIdentifiersList
            .find { userIdentifier -> userIdentifier.id == userIdentifierId }?.id

        if (managementSession == null
            || managementSession.userId != managementUserId
            || identifierIdToDelete == null
            || userIdentifiersList.size < 2
        ) {
            val error = UserError.CannotDeleteUserIdentifier()
            logAudit(
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
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
                resourceId = auditResourceId,
                status = AuditStatus.DENIED,
                metadata = auditMetadata + error.toDeniedUserAuditEventMetadata()
            )
            return AppResult.Error(error)
        }
        */

        val sessionsForDeletedIdentifier = sessionManager.getUserSessionsByIdentifierId(
            userIdentifierId = identifierIdToDelete,
            userId = userId
        ).dataOrNull() ?: emptyList()

        val deleteUserIdentifierResult = identifierManager.deleteUserIdentifier(
            userIdentifierId = identifierIdToDelete
        )

        when (deleteUserIdentifierResult) {
            is AppResult.Error -> {
                logAudit(
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    status = AuditStatus.FAILED,
                    metadata = auditMetadata + deleteUserIdentifierResult.error.toErrorUserAuditEventMetadata()
                )
            }
            is AppResult.Success -> {
                sessionsForDeletedIdentifier.forEach { session ->
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
                    resourceId = auditResourceId,
                    status = AuditStatus.SUCCESS,
                    metadata = auditMetadata
                )
            }
        }

        return deleteUserIdentifierResult
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
            action = UserAuditActionType.MANAGEMENT_DELETE_IDENTIFIER,
            resource = UserAuditResourceType.USER_IDENTIFIER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}
