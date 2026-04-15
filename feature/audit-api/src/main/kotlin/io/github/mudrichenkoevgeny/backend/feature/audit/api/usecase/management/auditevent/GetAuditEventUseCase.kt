package io.github.mudrichenkoevgeny.backend.feature.audit.api.usecase.management.auditevent

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.feature.audit.api.manager.AuditManager
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAuditEventUseCase @Inject constructor(
    private val userManager: UserManager,
    private val auditManager: AuditManager
) {
    suspend operator fun invoke(
        auditEventId: AuditEventId,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<AuditEvent> {
        val currentUserId = authenticatedRequestContext.userId

        val getCurrentUserResult = userManager.getUserById(currentUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val currentUser = when (getCurrentUserResult) {
            is AppResult.Error -> return getCurrentUserResult
            is AppResult.Success -> getCurrentUserResult.data
        }

        return auditManager.getEventById(
            eventId = auditEventId,
            userId = currentUserId,
            userPermissionCodes = currentUser.permissions
        ).mapNotNullOrError(
            CommonError.NotFound(
                resource = AuditEvent::class.java.simpleName,
                identifier = auditEventId.asHexDashString()
            )
        )
    }
}