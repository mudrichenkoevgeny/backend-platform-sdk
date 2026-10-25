package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.storage

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.storage.manager.StorageManager
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.audit.action.StorageAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.audit.resource.StorageAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.permission.StoragePermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for batch administrative deletion of multiple file records.
 *
 * Performs a soft delete in the database via [StorageManager] and records audit events.
 * Physical deletion from S3 is handled asynchronously by a background garbage collection job.
 *
 * @property storageManager Manager for storage file operations and soft deletion.
 * @property auditLogger Audit logging facility.
 */
@Singleton
class ManagementDeleteFilesUseCase @Inject constructor(
    private val storageManager: StorageManager,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val userManager: UserManager
) {

    /**
     * Executes batch administrative file deletion in the database.
     *
     * @param fileIds List of target file IDs.
     * @return [AppResult.Success] on completion, or [AppResult.Error] on failure.
     */
    suspend operator fun invoke(
        fileIds: List<FileId>,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<Unit> {
        val auditActorId = authenticatedRequestContext.userId.asHexDashString()
        val auditActorUserRole = authenticatedRequestContext.userRole
        val auditResourceIds = fileIds.map { it.asHexDashString() }
        val managementUserId = authenticatedRequestContext.userId

        val auditMetadata = authenticatedRequestContext.clientInfo.toAuditMetadata().toMutableSet()

        val managementUserResult = userManager.getUserInternalById(managementUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val managementUser = when (managementUserResult) {
            is AppResult.Success -> managementUserResult.data
            is AppResult.Error -> return handleError(
                error = managementUserResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceIds = auditResourceIds,
                baseMetadata = auditMetadata
            )
        }

        if (managementUser.accountStatus != UserAccountStatus.ACTIVE) {
            return handleError(
                error = UserError.UserIllegalAccountStatus(managementUserId),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceIds = auditResourceIds,
                baseMetadata = auditMetadata
            )
        }

        if (StoragePermissionCode.STORAGE_DELETE_OTHER_USERS_FILES !in managementUser.permissionCodes) {
            return handleError(
                error = UserError.UserMissingPermissions(managementUserId),
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceIds = auditResourceIds,
                baseMetadata = auditMetadata
            )
        }

        val deleteResult = storageManager.deleteStorageFilesBatch(fileIds)

        return when (deleteResult) {
            is AppResult.Success -> {
                logAudit(
                    status = AuditStatus.SUCCESS,
                    actorId = auditActorId,
                    actorUserRole = auditActorUserRole,
                    resourceIds = auditResourceIds,
                    metadata = auditMetadata
                )
                deleteResult
            }
            is AppResult.Error -> handleError(
                error = deleteResult.error,
                actorId = auditActorId,
                actorUserRole = auditActorUserRole,
                resourceIds = auditResourceIds,
                baseMetadata = auditMetadata
            )
        }
    }

    private fun <T> handleError(
        error: AppError,
        actorId: String,
        actorUserRole: UserRole,
        resourceIds: List<String>,
        baseMetadata: Set<AuditEventMetadata>
    ): AppResult<T> {
        val auditErrorLogData = auditErrorConverter.convert(error)
        logAudit(
            actorId = actorId,
            actorUserRole = actorUserRole,
            resourceIds = resourceIds,
            status = auditErrorLogData.status,
            metadata = baseMetadata + auditErrorLogData.metadata
        )
        return AppResult.Error(error)
    }

    private fun logAudit(
        actorId: String,
        actorUserRole: UserRole,
        resourceIds: List<String>,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>
    ) {
        for (resourceId in resourceIds) {
            auditLogger.log(
                actorId = actorId,
                actorType = AuditActorType.USER,
                actorUserRole = actorUserRole.value,
                action = StorageAuditActionType.MANAGEMENT_DELETE_FILE,
                resource = StorageAuditResourceType.STORAGE_FILE,
                resourceId = resourceId,
                status = status,
                metadata = metadata
            )
        }
    }
}