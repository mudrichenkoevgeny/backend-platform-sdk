package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.session

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSession
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagementGetSessionUseCase @Inject constructor(
    private val userManager: UserManager,
    private val sessionManager: SessionManager
) {
    /**
     * Retrieves specific [UserSession] details within the management scope.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY]
     * for the management caller.
     *
     * **Security:**
     * - Requires the management caller (STAFF or ADMIN) to be active.
     * - The [sessionManager] verifies if the caller has the required permissions (masked or unmasked)
     *   to view the session based on the target session owner's role.
     *
     * **Workflow:**
     * 1. Validates the management caller's existence and status.
     * 2. Fetches the [UserSession] via [sessionManager], which enforces visibility rules and
     *    permission-based masking of sensitive session data.
     *
     * @param userSessionId The ID of the session to retrieve.
     * @param authenticatedRequestContext The context of the authenticated management request.
     * @return [AppResult] containing the [UserSession] if found and authorized.
     */
    suspend operator fun invoke(
        userSessionId: UserSessionId,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<UserSession> {
        val managementUserId = authenticatedRequestContext.userId

        val getManagementUserResult = userManager.getUserByIdForSelf(managementUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val managementUser = when (getManagementUserResult) {
            is AppResult.Error -> return getManagementUserResult
            is AppResult.Success -> getManagementUserResult.data
        }

        if (managementUser.accountStatus != UserAccountStatus.ACTIVE) {
            return AppResult.Error(UserError.UserIllegalAccountStatus(managementUserId))
        }

        return sessionManager.getUserSessionForManagement(
            userSessionId = userSessionId,
            managementUserId = managementUserId,
            managementUserPermissionCodes = managementUser.permissionCodes
        ).mapNotNullOrError(
            CommonError.NotFound(
                resource = UserSession::class.java.simpleName,
                identifier = userSessionId.asHexDashString()
            )
        )
    }
}
