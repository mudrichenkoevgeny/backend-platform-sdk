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
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.metadata.UserAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.UserPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * Management use case for administratively deleting a user account
 * and revoking all active user sessions.
 *
 * Enforces authority level hierarchy, rate limiting, and MFA step-up
 * verification before removing user records and terminating active sessions.
 */
@Singleton
public class ManagementDeleteUserUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val userManager: UserManager,
    private val sessionManager: SessionManager,
    private val webSocketManager: WebSocketManager,
    private val authenticationChallengeService: AuthenticationChallengeService,
    private val clock: Clock
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
     *   [UserInternal.authorityLevel] than the target user.
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
    public suspend operator fun invoke(
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

        val checksResult = performPreChecks(
            managementUserId = managementUserId,
            userId = userId,
            auditActorId = auditActorId,
            auditActorUserRole = auditActorUserRole,
            auditResourceId = auditResourceId,
            auditMetadata = auditMetadata
        )
        if (checksResult is AppResult.Error) return checksResult
        val managementUser = (checksResult as AppResult.Success).data

        val mfaResult = verifyMfa(
            authenticatedRequestContext = authenticatedRequestContext,
            managementUser = managementUser,
            auditActorId = auditActorId,
            auditActorUserRole = auditActorUserRole,
            auditResourceId = auditResourceId,
            auditMetadata = auditMetadata
        )
        if (mfaResult is AppResult.Error) return mfaResult

        val sessionsToDelete = sessionManager.getAllUserSessionsInternalList(
            userId = userId
        ).dataOrNull() ?: emptyList()

        val deleteResult = userManager.deleteUser(userId)

        return when (deleteResult) {
            is AppResult.Success -> {
                sessionsToDelete.forEach { session ->
                    webSocketManager.sendMessageToUserSession(
                        userSessionId = session.id,
                        frame = SocketFrame(
                            type = UserWebSocketEventTypes.SESSION_DELETED,
                            payload = null,
                            metadata = emptyMap(),
                            timestamp = clock.now().toEpochMilliseconds()
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

    private suspend fun performPreChecks(
        managementUserId: UserId,
        userId: UserId,
        auditActorId: String,
        auditActorUserRole: UserRole,
        auditResourceId: String,
        auditMetadata: MutableSet<AuditEventMetadata>
    ): AppResult<UserInternal> {
        if (managementUserId == userId) {
            return handleError(
                UserError.UserForbidden(managementUserId),
                auditActorId,
                auditActorUserRole,
                auditResourceId,
                auditMetadata
            )
        }

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserManagementRateLimitAction.MANAGEMENT_USER_DELETE,
            identifier = managementUserId.asHexDashString()
        )
        if (rateLimitCheck is AppResult.Error) {
            return handleError(
                rateLimitCheck.error,
                auditActorId,
                auditActorUserRole,
                auditResourceId,
                auditMetadata
            )
        }

        val managementUserResult = userManager.getUserInternalById(managementUserId)
            .mapNotNullOrError(UserError.UserForbidden())
        val managementUser = when (managementUserResult) {
            is AppResult.Success -> managementUserResult.data
            is AppResult.Error -> return handleError(
                managementUserResult.error,
                auditActorId,
                auditActorUserRole,
                auditResourceId,
                auditMetadata
            )
        }

        if (managementUser.accountStatus != UserAccountStatus.ACTIVE) {
            return handleError(
                UserError.UserIllegalAccountStatus(managementUserId),
                auditActorId,
                auditActorUserRole,
                auditResourceId,
                auditMetadata
            )
        }

        val targetUserResult = userManager.getUserInternalById(userId).mapNotNullOrError(UserError.UserNotFound(userId))
        val targetUser = when (targetUserResult) {
            is AppResult.Success -> targetUserResult.data
            is AppResult.Error -> return handleError(
                targetUserResult.error,
                auditActorId,
                auditActorUserRole,
                auditResourceId,
                auditMetadata
            )
        }

        if (targetUser.authorityLevel >= managementUser.authorityLevel) {
            return handleError(
                UserError.UserInsufficientAuthorityLevel(managementUserId),
                auditActorId,
                auditActorUserRole,
                auditResourceId,
                auditMetadata
            )
        }

        val requiredPermission = when (targetUser.role) {
            UserRole.USER -> UserPermissionCode.USER_DELETE_FOR_USER
            UserRole.STAFF -> UserPermissionCode.USER_DELETE_FOR_STAFF
            UserRole.ADMIN -> return handleError(
                UserError.UserForbidden(managementUserId),
                auditActorId,
                auditActorUserRole,
                auditResourceId,
                auditMetadata
            )
        }

        if (requiredPermission !in managementUser.permissionCodes) {
            return handleError(
                UserError.UserMissingPermissions(managementUserId),
                auditActorId,
                auditActorUserRole,
                auditResourceId,
                auditMetadata
            )
        }

        return AppResult.Success(managementUser)
    }

    private suspend fun verifyMfa(
        authenticatedRequestContext: AuthenticatedRequestContext,
        managementUser: UserInternal,
        auditActorId: String,
        auditActorUserRole: UserRole,
        auditResourceId: String,
        auditMetadata: MutableSet<AuditEventMetadata>
    ): AppResult<Unit> {
        val managementUserSessionResult = sessionManager.getUserSessionInternal(
            userSessionId = authenticatedRequestContext.sessionId
        ).mapNotNullOrError(UserError.InvalidSession())

        val managementUserSession = when (managementUserSessionResult) {
            is AppResult.Success -> managementUserSessionResult.data
            is AppResult.Error -> return handleError(
                managementUserSessionResult.error,
                auditActorId,
                auditActorUserRole,
                auditResourceId,
                auditMetadata
            )
        }

        val ensureSessionConfirmedResult = authenticationChallengeService.ensureSessionConfirmed(
            userInternal = managementUser,
            userSession = managementUserSession,
            requireTotp = true
        )
        if (ensureSessionConfirmedResult is AppResult.Error) {
            return handleError(
                ensureSessionConfirmedResult.error,
                auditActorId,
                auditActorUserRole,
                auditResourceId,
                auditMetadata
            )
        }
        return AppResult.Success(Unit)
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
            actorUserRole = actorUserRole.value,
            action = UserAuditActionType.MANAGEMENT_DELETE_USER,
            resource = UserAuditResourceType.USER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}
