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
import io.github.mudrichenkoevgeny.shared.foundation.feature.audit.api.domain.permissions.AuditPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAuditEventUseCase @Inject constructor(
    private val userManager: UserManager,
    private val auditManager: AuditManager
) {
    /**
     * Retrieves details of a specific audit event.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY]
     * for the management caller.
     *
     * **Security:**
     * - Requires the management caller (STAFF or ADMIN) to be in an allowed status.
     * - The [auditManager] enforces visibility rules based on the caller's permissions and the
     *   actor type of the event (e.g., USER, SYSTEM, STAFF, ADMIN).
     * - Metadata or actor identifiers may be masked based on specific [AuditPermissionCode] levels.
     *
     * **Workflow:**
     * 1. Validates the existence and status of the management caller.
     * 2. Fetches the [AuditEvent] via [auditManager], which handles permission-based filtering
     *    and data masking.
     *
     * @param auditEventId The unique ID of the audit event to retrieve.
     * @param authenticatedRequestContext The context of the authenticated management caller.
     * @return [AppResult] containing the [AuditEvent] if found and authorized.
     */
    suspend operator fun invoke(
        auditEventId: AuditEventId,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<AuditEvent> {
        val currentUserId = authenticatedRequestContext.userId

        val getCurrentUserResult = userManager.getUserByIdForSelf(currentUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val currentUser = when (getCurrentUserResult) {
            is AppResult.Error -> return getCurrentUserResult
            is AppResult.Success -> getCurrentUserResult.data
        }

        return auditManager.getEventById(
            eventId = auditEventId,
            userId = currentUserId,
            userPermissionCodes = currentUser.permissionCodes
        ).mapNotNullOrError(
            CommonError.NotFound(
                resource = AuditEvent::class.java.simpleName,
                identifier = auditEventId.asHexDashString()
            )
        )
    }
}