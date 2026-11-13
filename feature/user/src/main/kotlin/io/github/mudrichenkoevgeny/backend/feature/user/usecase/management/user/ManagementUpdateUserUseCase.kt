package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.dataOrNull
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.UpdateUserParams
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.websocket.manager.WebSocketManager
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserManagementRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.backend.feature.user.validator.user.UserUpdateValidator
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
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserPrivate
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.UserPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.user.toUserPrivate
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserWebSocketEventTypes
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * Management use case for updating account statuses, roles, authority levels,
 * permissions, or profile details for a user account.
 *
 * Enforces authority level hierarchy, permission checks, rate limiting, and
 * MFA step-up verification before applying updates.
 */
@Singleton
public class ManagementUpdateUserUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val userManager: UserManager,
    private val sessionManager: SessionManager,
    private val webSocketManager: WebSocketManager,
    private val authenticationChallengeService: AuthenticationChallengeService,
    private val userUpdateValidator: UserUpdateValidator,
    private val clock: Clock
) {
    /**
     * Updates account status, authority level, permissions, or profile details for a target user.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE] for the management caller.
     *
     * **Security:**
     * - Requires an active session for the management caller (STAFF or ADMIN) with sufficient permissions.
     * - Enforces MFA step-up verification if the session is stale or security-sensitive.
     * - Validates that the management caller's authority level is strictly greater than the target's.
     * - Prevents self-modification of management fields.
     * - Protects against abuse via [UserManagementRateLimitAction.MANAGEMENT_USER_UPDATE].
     *
     * **Workflow:**
     * 1. Validates the management caller's account status and rate limits.
     * 2. Checks authority level hierarchy between the management caller and target user.
     * 3. Determines required permissions based on the specific fields being updated and target role.
     * 4. Validates nickname and display name via [userUpdateValidator].
     * 5. Ensures the management caller's session is confirmed via [authenticationChallengeService].
     * 6. Performs the update via [userManager].
     * 7. Notifies the target user's active sessions via [webSocketManager] using [UserWebSocketEventTypes.USER_UPDATED].
     * 8. Logs the action via [AuditLogger] with [UserAuditActionType.MANAGEMENT_UPDATE_USER].
     *
     * @param userId The unique identifier of the target user to update.
     * @param updateUserParams The structured update parameters holder ([UpdateUserParams]).
     * @param authenticatedRequestContext The context of the authenticated management request.
     * @return [AppResult] containing the updated [UserPrivate].
     */
    public suspend operator fun invoke(
        userId: UserId,
        updateUserParams: UpdateUserParams,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<UserPrivate> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditResourceId = userId.asHexDashString()
        val managementUserId = authenticatedRequestContext.userId

        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata().toMutableSet()

        val preChecksResult = performPreChecks(
            managementUserId = managementUserId,
            userId = userId,
            auditActorId = auditActorId,
            auditActorUserRole = auditActorUserRole,
            auditResourceId = auditResourceId,
            auditMetadata = auditMetadata
        )
        if (preChecksResult is AppResult.Error) return preChecksResult
        val (managementUser, targetUser) = (preChecksResult as AppResult.Success).data

        val permissionResult = checkPermissions(
            updateUserParams = updateUserParams,
            managementUser = managementUser,
            targetUser = targetUser,
            managementUserId = managementUserId,
            auditActorId = auditActorId,
            auditActorUserRole = auditActorUserRole,
            auditResourceId = auditResourceId,
            auditMetadata = auditMetadata
        )
        if (permissionResult is AppResult.Error) return permissionResult

        val mfaResult = verifyMfa(
            authenticatedRequestContext = authenticatedRequestContext,
            managementUser = managementUser,
            auditActorId = auditActorId,
            auditActorUserRole = auditActorUserRole,
            auditResourceId = auditResourceId,
            auditMetadata = auditMetadata
        )
        if (mfaResult is AppResult.Error) return mfaResult

        val userValidationResult = userUpdateValidator.validate(
            user = targetUser,
            updateUserParams = updateUserParams
        )
        if (userValidationResult is AppResult.Error) {
            return handleError(
                userValidationResult.error,
                auditActorId,
                auditActorUserRole,
                auditResourceId,
                auditMetadata
            )
        }

        val updateResult = userManager.updateUser(
            user = targetUser,
            updateUserParams = updateUserParams
        ).mapNotNullOrError(UserError.UserNotFound(targetUser.id))

        return when (updateResult) {
            is AppResult.Success -> {
                val sessionsForUpdatedUser = sessionManager.getAllUserSessionsInternalList(
                    userId = userId
                ).dataOrNull() ?: emptyList()

                sessionsForUpdatedUser.forEach { session ->
                    webSocketManager.sendMessageToUserSession(
                        userSessionId = session.id,
                        frame = SocketFrame(
                            type = UserWebSocketEventTypes.USER_UPDATED,
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
                AppResult.Success(updateResult.data.toUserPrivate())
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

    private suspend fun performPreChecks(
        managementUserId: UserId,
        userId: UserId,
        auditActorId: String,
        auditActorUserRole: UserRole,
        auditResourceId: String,
        auditMetadata: MutableSet<AuditEventMetadata>
    ): AppResult<Pair<UserInternal, UserInternal>> {
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
            action = UserManagementRateLimitAction.MANAGEMENT_USER_UPDATE,
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

        return AppResult.Success(Pair(managementUser, targetUser))
    }

    private fun checkPermissions(
        updateUserParams: UpdateUserParams,
        managementUser: UserInternal,
        targetUser: UserInternal,
        managementUserId: UserId,
        auditActorId: String,
        auditActorUserRole: UserRole,
        auditResourceId: String,
        auditMetadata: MutableSet<AuditEventMetadata>
    ): AppResult<Unit> {
        val requiredPermissions = mutableSetOf<PermissionCode>()

        val statusResult = checkAccountStatusPermission(
            updateUserParams,
            targetUser,
            managementUserId,
            auditActorId,
            auditActorUserRole,
            auditResourceId,
            auditMetadata,
            requiredPermissions
        )
        if (statusResult is AppResult.Error) return statusResult

        val authorityResult = checkAuthorityLevelPermission(
            updateUserParams,
            managementUser,
            targetUser,
            managementUserId,
            auditActorId,
            auditActorUserRole,
            auditResourceId,
            auditMetadata,
            requiredPermissions
        )
        if (authorityResult is AppResult.Error) return authorityResult

        val permissionsResult = checkPermissionsFieldPermission(
            updateUserParams,
            managementUser,
            targetUser,
            managementUserId,
            auditActorId,
            auditActorUserRole,
            auditResourceId,
            auditMetadata,
            requiredPermissions
        )
        if (permissionsResult is AppResult.Error) return permissionsResult

        val securityResult = checkSecurityFieldsPermission(
            updateUserParams,
            targetUser,
            managementUserId,
            auditActorId,
            auditActorUserRole,
            auditResourceId,
            auditMetadata,
            requiredPermissions
        )
        if (securityResult is AppResult.Error) return securityResult

        return validateRequiredPermissions(
            managementUser,
            managementUserId,
            auditActorId,
            auditActorUserRole,
            auditResourceId,
            auditMetadata,
            requiredPermissions
        )
    }

    private fun checkAccountStatusPermission(
        updateUserParams: UpdateUserParams,
        targetUser: UserInternal,
        managementUserId: UserId,
        auditActorId: String,
        auditActorUserRole: UserRole,
        auditResourceId: String,
        auditMetadata: MutableSet<AuditEventMetadata>,
        requiredPermissions: MutableSet<PermissionCode>
    ): AppResult<Unit> {
        val targetAccountStatus = (updateUserParams.accountStatus as? UpdateField.Set)?.value
        if (targetAccountStatus != null) {
            val updateStatusPermission = when (targetUser.role) {
                UserRole.USER -> UserPermissionCode.USER_UPDATE_STATUS_FOR_USER
                UserRole.STAFF -> UserPermissionCode.USER_UPDATE_STATUS_FOR_STAFF
                else -> return handleError(
                    UserError.UserForbidden(managementUserId),
                    auditActorId,
                    auditActorUserRole,
                    auditResourceId,
                    auditMetadata
                )
            }
            requiredPermissions.add(updateStatusPermission)
        }
        return AppResult.Success(Unit)
    }

    private fun checkAuthorityLevelPermission(
        updateUserParams: UpdateUserParams,
        managementUser: UserInternal,
        targetUser: UserInternal,
        managementUserId: UserId,
        auditActorId: String,
        auditActorUserRole: UserRole,
        auditResourceId: String,
        auditMetadata: MutableSet<AuditEventMetadata>,
        requiredPermissions: MutableSet<PermissionCode>
    ): AppResult<Unit> {
        val targetAuthorityLevel = (updateUserParams.authorityLevel as? UpdateField.Set)?.value
        if (targetAuthorityLevel != null) {
            if (targetAuthorityLevel >= managementUser.authorityLevel) {
                return handleError(
                    UserError.UserInsufficientAuthorityLevel(managementUserId),
                    auditActorId,
                    auditActorUserRole,
                    auditResourceId,
                    auditMetadata
                )
            }
            val updateAuthorityLevelPermission = when (targetUser.role) {
                UserRole.USER -> UserPermissionCode.USER_UPDATE_AUTHORITY_FOR_USER
                UserRole.STAFF -> UserPermissionCode.USER_UPDATE_AUTHORITY_FOR_STAFF
                else -> return handleError(
                    UserError.UserForbidden(managementUserId),
                    auditActorId,
                    auditActorUserRole,
                    auditResourceId,
                    auditMetadata
                )
            }
            requiredPermissions.add(updateAuthorityLevelPermission)
        }
        return AppResult.Success(Unit)
    }

    private fun checkPermissionsFieldPermission(
        updateUserParams: UpdateUserParams,
        managementUser: UserInternal,
        targetUser: UserInternal,
        managementUserId: UserId,
        auditActorId: String,
        auditActorUserRole: UserRole,
        auditResourceId: String,
        auditMetadata: MutableSet<AuditEventMetadata>,
        requiredPermissions: MutableSet<PermissionCode>
    ): AppResult<Unit> {
        val targetPermissions = (updateUserParams.permissions as? UpdateField.Set)?.value
        if (targetPermissions != null) {
            val updatePermissionsPermission = when (targetUser.role) {
                UserRole.USER -> UserPermissionCode.USER_UPDATE_PERMISSIONS_FOR_USER
                UserRole.STAFF -> UserPermissionCode.USER_UPDATE_PERMISSIONS_FOR_STAFF
                else -> return handleError(
                    UserError.UserForbidden(managementUserId),
                    auditActorId,
                    auditActorUserRole,
                    auditResourceId,
                    auditMetadata
                )
            }
            requiredPermissions.add(updatePermissionsPermission)

            val forbiddenManagementPermissions = targetPermissions.filter {
                it !in managementUser.permissionCodes
            }
            if (forbiddenManagementPermissions.isNotEmpty()) {
                return handleError(
                    UserError.UserMissingPermissions(managementUserId),
                    auditActorId,
                    auditActorUserRole,
                    auditResourceId,
                    auditMetadata
                )
            }
        }
        return AppResult.Success(Unit)
    }

    private fun checkSecurityFieldsPermission(
        updateUserParams: UpdateUserParams,
        targetUser: UserInternal,
        managementUserId: UserId,
        auditActorId: String,
        auditActorUserRole: UserRole,
        auditResourceId: String,
        auditMetadata: MutableSet<AuditEventMetadata>,
        requiredPermissions: MutableSet<PermissionCode>
    ): AppResult<Unit> {
        if (updateUserParams.lockoutType !is UpdateField.Ignore ||
            updateUserParams.temporaryLockoutUntil !is UpdateField.Ignore ||
            updateUserParams.storageConfiguration !is UpdateField.Ignore
        ) {
            val updateSecurityPermission = when (targetUser.role) {
                UserRole.USER -> UserPermissionCode.USER_UPDATE_SECURITY_FOR_USER
                UserRole.STAFF -> UserPermissionCode.USER_UPDATE_SECURITY_FOR_STAFF
                else -> return handleError(
                    UserError.UserForbidden(managementUserId),
                    auditActorId,
                    auditActorUserRole,
                    auditResourceId,
                    auditMetadata
                )
            }
            requiredPermissions.add(updateSecurityPermission)
        }
        return AppResult.Success(Unit)
    }

    private fun validateRequiredPermissions(
        managementUser: UserInternal,
        managementUserId: UserId,
        auditActorId: String,
        auditActorUserRole: UserRole,
        auditResourceId: String,
        auditMetadata: MutableSet<AuditEventMetadata>,
        requiredPermissions: Set<PermissionCode>
    ): AppResult<Unit> {
        val missingRequiredPermissions = requiredPermissions.filter {
            it !in managementUser.permissionCodes
        }
        if (missingRequiredPermissions.isNotEmpty()) {
            return handleError(
                UserError.UserMissingPermissions(managementUserId),
                auditActorId,
                auditActorUserRole,
                auditResourceId,
                auditMetadata
            )
        }
        return AppResult.Success(Unit)
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
                error = managementUserSessionResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        val ensureSessionConfirmedResult = authenticationChallengeService.ensureSessionConfirmed(
            userInternal = managementUser,
            userSession = managementUserSession,
            requireTotp = true
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
            action = UserAuditActionType.MANAGEMENT_UPDATE_USER,
            resource = UserAuditResourceType.USER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}
