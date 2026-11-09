package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.UpdateUserParams
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.validator.user.UserUpdateValidator
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserPrivate
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.user.toUserPrivate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Open use case for an authenticated user to update their own profile details (nickname, display name, avatar, birthdate).
 *
 * Enforces account status checks and profile field validation before persisting updates.
 */
@Singleton
public class UpdateSelfUserUseCase @Inject constructor(
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val userManager: UserManager,
    private val userUpdateValidator: UserUpdateValidator
) {
    /**
     * Updates the profile details (nickname, display name, avatar, birthdate) for the authenticated user.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE].
     *
     * **Workflow:**
     * 1. Validates the existence and [UserAccountStatus.ACTIVE] status of the authenticated caller.
     * 2. Validates new nickname and display name via [userUpdateValidator].
     * 3. Updates profile fields via [userManager].
     * 4. Logs the operation via [AuditLogger] with [UserAuditActionType.SELF_UPDATE_USER].
     *
     * @param updateUserParams Mapped domain update parameters.
     * @param authenticatedRequestContext Context of the authenticated user request.
     * @return [AppResult] containing the updated [UserPrivate].
     */
    public suspend operator fun invoke(
        updateUserParams: UpdateUserParams,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<UserPrivate> {
        val currentUserId = authenticatedRequestContext.userId
        val auditActorId = currentUserId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditResourceId = currentUserId.asHexDashString()

        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata().toMutableSet()

        val currentUserResult = userManager.getUserInternalById(currentUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val currentUser = when (currentUserResult) {
            is AppResult.Success -> currentUserResult.data
            is AppResult.Error -> return handleError(
                error = currentUserResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        if (currentUser.accountStatus != UserAccountStatus.ACTIVE) {
            return handleError(
                error = UserError.UserIllegalAccountStatus(currentUserId),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        val userValidationResult = userUpdateValidator.validate(
            user = currentUser,
            updateUserParams = updateUserParams
        )
        if (userValidationResult is AppResult.Error) {
            return handleError(
                error = userValidationResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceId = auditResourceId,
                baseMetadata = auditMetadata
            )
        }

        val updateResult = userManager.updateUser(
            user = currentUser,
            updateUserParams = updateUserParams
        ).mapNotNullOrError(UserError.UserNotFound(currentUser.id))

        return when (updateResult) {
            is AppResult.Success -> {
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
            action = UserAuditActionType.SELF_UPDATE_USER,
            resource = UserAuditResourceType.USER,
            resourceId = resourceId,
            status = status,
            metadata = metadata
        )
    }
}
