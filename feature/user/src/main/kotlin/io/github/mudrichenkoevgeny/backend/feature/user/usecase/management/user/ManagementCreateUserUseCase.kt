package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.user

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.core.security.usecase.open.passwordpolicy.ValidatePasswordUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserManagementRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge.AuthenticationChallengeService
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.metadata.UserAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.permission.UserPermissionCode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagementCreateUserUseCase @Inject constructor(
    private val rateLimiter: RateLimiter,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val userManager: UserManager,
    private val authManager: AuthManager,
    private val sessionManager: SessionManager,
    private val validatePasswordUseCase: ValidatePasswordUseCase,
    private val authenticationChallengeService: AuthenticationChallengeService
) {
    /**
     * Manually creates a new user with specific roles, permissions, and authority levels.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE] for the management caller.
     *
     * **Security:**
     * - Requires an active management session with appropriate creation permissions.
     * - Enforces MFA step-up verification for the management caller via [authenticationChallengeService].
     * - Validates the new user's password against the system's [ValidatePasswordUseCase] policy.
     * - Restricts the [authorityLevel] and [permissionCodes] of the new user to be within
     *   the bounds of the management caller's own authority and permissions.
     * - Protects against resource exhaustion via [UserManagementRateLimitAction.MANAGEMENT_USER_CREATE].
     *
     * **Workflow:**
     * 1. Validates the management caller's session, account status, and rate limits.
     * 2. Verifies that the management caller has the required [UserPermissionCode] based on the target [role].
     * 3. Ensures the management caller is not granting permissions or authority levels they do not possess.
     * 4. Validates the provided [password] policy.
     * 5. Confirms the management session via [authenticationChallengeService].
     * 6. Creates the user and their authentication identity via [authManager].
     * 7. Logs the creation event via [AuditLogger] with [UserAuditActionType.MANAGEMENT_CREATE_USER].
     *
     * @param email The email address for the new user.
     * @param password The plaintext password for the new user.
     * @param role The assigned role.
     * @param accountStatus The initial status for the new user.
     * @param authorityLevel The numeric authority level assigned to the user.
     * @param permissionCodes The set of functional permissions granted to the user.
     * @param authenticatedRequestContext The context of the authenticated management request.
     * @return [AppResult] containing the [UserDetails] of the created user.
     */
    suspend operator fun invoke(
        email: String,
        password: String,
        role: UserRole,
        accountStatus: UserAccountStatus,
        authorityLevel: Int,
        permissionCodes: Set<PermissionCode>,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<UserDetails> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val managementUserId = authenticatedRequestContext.userId

        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata().toMutableSet()
        auditMetadata.add(
            AuditEventMetadata(
                key = UserAuditMetadataKey.EMAIL_ADDRESS,
                value = email
            )
        )

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserManagementRateLimitAction.MANAGEMENT_USER_CREATE,
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

        if (authorityLevel >= managementUser.authorityLevel) {
            return handleError(
                error = UserError.UserInsufficientAuthorityLevel(managementUserId),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val requiredPermission = when (role) {
            UserRole.USER -> UserPermissionCode.USER_CREATE_AS_USER
            UserRole.STAFF -> UserPermissionCode.USER_CREATE_AS_STAFF
            else -> return handleError(
                error = UserError.UserForbidden(managementUserId),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        if (requiredPermission !in managementUser.permissionCodes
            || !managementUser.permissionCodes.containsAll(permissionCodes)
        ) {
            return handleError(
                error = UserError.UserMissingPermissions(managementUserId),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                baseMetadata = auditMetadata
            )
        }

        val passwordPolicyCheckResult = validatePasswordUseCase(password)
        if (passwordPolicyCheckResult is AppResult.Error) {
            return handleError(
                error = passwordPolicyCheckResult.error,
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

        val createUserResult = authManager.createUserAndIdentifier(
            userAuthProvider = UserAuthProvider.EMAIL,
            identifier = email,
            password = password,
            roleForUserCreation = role,
            accountStatusForUserCreation = accountStatus,
            authorityLevelForUserCreation = authorityLevel,
            permissionCodesForUserCreation = permissionCodes
        )

        return when (createUserResult) {
            is AppResult.Success -> {
                logAudit(
                    status = AuditStatus.SUCCESS,
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceId = createUserResult.data.id.asHexDashString(),
                    metadata = auditMetadata
                )
                createUserResult
            }
            is AppResult.Error -> handleError(
                error = createUserResult.error,
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
        resourceId: String? = null,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>
    ) {
        auditLogger.log(
            actorId = actorId,
            actorType = AuditActorType.USER,
            actorUserRole = actorUserRole.serialName,
            action = UserAuditActionType.MANAGEMENT_CREATE_USER,
            resource = UserAuditResourceType.USER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}