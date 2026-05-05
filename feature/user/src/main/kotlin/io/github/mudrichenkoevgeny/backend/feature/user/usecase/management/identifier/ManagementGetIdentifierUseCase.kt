package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifier
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagementGetIdentifierUseCase @Inject constructor(
    private val userManager: UserManager,
    private val identifierManager: IdentifierManager
) {
    /**
     * Retrieves specific [UserIdentifier] details within the management scope.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY]
     * for the management caller.
     *
     * **Security:**
     * - Requires the management caller (STAFF or ADMIN) to be in an allowed status.
     * - The [identifierManager] verifies if the caller has the required permissions
     *   (masked or unmasked) to view the identifier based on the owner's role.
     *
     * **Workflow:**
     * 1. Validates the existence and status of the management caller.
     * 2. Fetches the [UserIdentifier] via [identifierManager], which enforces visibility
     *    rules and permission-based masking of the identifier value.
     *
     * @param userIdentifierId The ID of the identifier to retrieve.
     * @param authenticatedRequestContext The context of the authenticated management request.
     * @return [AppResult] containing the [UserIdentifier] if found and authorized.
     */
    suspend operator fun invoke(
        userIdentifierId: UserIdentifierId,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<UserIdentifier> {
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

        return identifierManager.getUserIdentifierByIdForManagement(
            userIdentifierId = userIdentifierId,
            managementUserId = managementUserId,
            managementUserPermissionCodes = managementUser.permissionCodes
        ).mapNotNullOrError(
            CommonError.NotFound(
                resource = UserIdentifier::class.java.simpleName,
                identifier = userIdentifierId.asHexDashString()
            )
        )
    }
}
