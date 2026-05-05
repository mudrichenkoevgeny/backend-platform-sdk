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
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.websocket.SocketFrame
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.UserPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

@Singleton
class ManagementUpdateUserUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val userManager: UserManager,
    private val sessionManager: SessionManager,
    private val webSocketManager: WebSocketManager,
    private val authenticationChallengeService: AuthenticationChallengeService
) {
    /**
     * Updates account status, authority level, or permissions for a target user.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE] for the management caller.
     *
     * **Security:**
     * - Requires an active session for the management caller (STAFF or ADMIN) with sufficient permissions.
     * - Enforces MFA step-up verification if the session is stale or security-sensitive.
     * - Validates that the management caller's [authorityLevel] is strictly greater than the target's.
     * - Prevents self-modification of management fields.
     * - Protects against abuse via [UserManagementRateLimitAction.MANAGEMENT_USER_UPDATE].
     *
     * **Workflow:**
     * 1. Validates the management caller's account status and rate limits.
     * 2. Checks authority level hierarchy between the management caller and target.
     * 3. Determines required permissions based on the specific fields being updated and target role.
     * 4. Ensures the management caller's session is confirmed via [authenticationChallengeService].
     * 5. Performs the update via [userManager].
     * 6. Notifies the target user's active sessions via [webSocketManager] using [UserWebSocketEventTypes.USER_UPDATED].
     * 7. Logs the action via [AuditLogger] with [UserAuditActionType.MANAGEMENT_UPDATE_USER].
     *
     * @param userId The ID of the target user to update.
     * @param accountStatus The new account status, if provided.
     * @param authorityLevel The new authority level, if provided.
     * @param permissionCodes The new set of permission codes, if provided.
     * @param authenticatedRequestContext The context of the authenticated management request.
     * @return [AppResult] containing the updated [UserDetails].
     */
    suspend operator fun invoke(
        userId: UserId,
        accountStatus: UserAccountStatus? = null,
        authorityLevel: Int? = null,
        permissionCodes: Set<PermissionCode>? = null,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<UserDetails> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditResourceId = userId.asHexDashString()
        val managementUserId = authenticatedRequestContext.userId

        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata().toMutableSet()

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
            action = UserManagementRateLimitAction.MANAGEMENT_USER_UPDATE,
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

        val requiredPermissions = mutableSetOf<PermissionCode>()

        if (accountStatus != null) {
            val updateStatusPermission = when (targetUser.role) {
                UserRole.USER -> UserPermissionCode.USER_UPDATE_STATUS_FOR_USER
                UserRole.STAFF -> UserPermissionCode.USER_UPDATE_STATUS_FOR_STAFF
                else -> return handleError(
                    error = UserError.UserForbidden(managementUserId),
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    baseMetadata = auditMetadata
                )
            }
            requiredPermissions.add(updateStatusPermission)
        }

        if (authorityLevel != null) {
            if (authorityLevel >= managementUser.authorityLevel) {
                return handleError(
                    error = UserError.UserInsufficientAuthorityLevel(managementUserId),
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    baseMetadata = auditMetadata
                )
            }
            val updateAuthorityLevelPermission = when (targetUser.role) {
                UserRole.USER -> UserPermissionCode.USER_UPDATE_AUTHORITY_FOR_USER
                UserRole.STAFF -> UserPermissionCode.USER_UPDATE_AUTHORITY_FOR_STAFF
                else -> return handleError(
                    error = UserError.UserForbidden(managementUserId),
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    baseMetadata = auditMetadata
                )
            }
            requiredPermissions.add(updateAuthorityLevelPermission)
        }

        if (permissionCodes != null) {
            val updatePermissionsPermission = when (targetUser.role) {
                UserRole.USER -> UserPermissionCode.USER_UPDATE_PERMISSIONS_FOR_USER
                UserRole.STAFF -> UserPermissionCode.USER_UPDATE_PERMISSIONS_FOR_STAFF
                else -> return handleError(
                    error = UserError.UserForbidden(managementUserId),
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    baseMetadata = auditMetadata
                )
            }
            requiredPermissions.add(updatePermissionsPermission)

            val forbiddenManagementPermissions = permissionCodes.filter { it !in managementUser.permissionCodes }
            if (forbiddenManagementPermissions.isNotEmpty()) {
                return handleError(
                    error = UserError.UserMissingPermissions(managementUserId),
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = auditResourceId,
                    baseMetadata = auditMetadata
                )
            }
        }

        val missingRequiredPermissions = requiredPermissions.filter { it !in managementUser.permissionCodes }
        if (missingRequiredPermissions.isNotEmpty()) {
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

        val updateResult = userManager.updateUserForManagement(
            user = targetUser,
            accountStatus = accountStatus,
            authorityLevel = authorityLevel,
            permissions = permissionCodes
        ).mapNotNullOrError(UserError.UserNotFound(targetUser.id))

        return when (updateResult) {
            is AppResult.Success -> {
                val sessionsForUpdatedUser = sessionManager.getAllUserSessions(
                    userId = userId
                ).dataOrNull() ?: emptyList()

                sessionsForUpdatedUser.forEach { session ->
                    webSocketManager.sendMessageToUserSession(
                        userSessionId = session.id,
                        frame = SocketFrame(
                            type = UserWebSocketEventTypes.USER_UPDATED,
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
                updateResult
            }
            is AppResult.Error -> handleError(
                error = updateResult.error,
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
            action = UserAuditActionType.MANAGEMENT_UPDATE_USER,
            resource = UserAuditResourceType.USER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}