package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user

import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import javax.inject.Inject
import javax.inject.Singleton

// todo refactor
@Singleton
class RestoreUserUseCase @Inject constructor(
    private val auditLogger: AuditLogger,
    private val userManager: UserManager
) {
    suspend operator fun invoke(
        userId: UserId,
        requestContext: RequestContext
    ): AppResult<Unit> {
        val auditMetadata: Set<AuditEventMetadata> = requestContext.clientInfo.toAuditMetadata()

        val currentUserId = requestContext.userId
        if (currentUserId == null) {
            logAudit(
                status = AuditStatus.DENIED,
                metadata = auditMetadata
            )
            return AppResult.Error(UserError.UserForbidden())
        }

        val getCurrentUserResult = userManager.getUserById(currentUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val currentUser = when (getCurrentUserResult) {
            is AppResult.Error -> {
                logAudit(
                    status = AuditStatus.DENIED,
                    metadata = auditMetadata
                )
                return getCurrentUserResult
            }
            is AppResult.Success -> getCurrentUserResult.data
        }

        val metadataActorId = currentUser.id.asHexDashString()

        if (currentUser.id != userId) {
            logAudit(
                actorId = metadataActorId,
                status = AuditStatus.DENIED,
                metadata = auditMetadata
            )
            return AppResult.Error(UserError.UserForbidden())
        }

        // todo wait dor userManager
//        userManager.
    }

    private fun logAudit(
        actorId: String? = null,
        status: AuditStatus,
        metadata: Set<AuditEventMetadata>
    ) {
        auditLogger.log(
            actorId = actorId,
            actorType = AuditActorType.USER,
            actorUserRole = null,
            action = UserAuditActionType.SELF_RESTORE_USER,
            resource = UserAuditResourceType.USER,
            status = status,
            metadata = metadata
        )
    }
}