package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.identifier

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
class GetIdentifierUseCase @Inject constructor(
    private val userManager: UserManager,
    private val identifierManager: IdentifierManager
) {
    /**
     * Retrieves specific identifier details by its ID for the current authenticated user.
     *
     * **Allowed Account Statuses:** [UserAccountStatus.ACTIVE], [UserAccountStatus.READ_ONLY].
     *
     * **Workflow:**
     * 1. Validates the existence and accessibility of the user via [UserManager].
     * 2. Verifies that the account status permits data retrieval.
     * 3. Fetches the identifier details via [IdentifierManager.getUserIdentifierByIdForSelf].
     * 4. Ensures the identifier exists and belongs to the caller.
     *
     * @param identifierId The unique identifier of the requested record.
     * @param authenticatedRequestContext The context of the authenticated request.
     * @return [AppResult] containing the [UserIdentifier] or a [CommonError.NotFound] error.
     */
    suspend operator fun invoke(
        identifierId: UserIdentifierId,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<UserIdentifier> {
        val currentUserId = authenticatedRequestContext.userId

        val getCurrentUserResult = userManager.getUserByIdForSelf(currentUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val currentUser = when (getCurrentUserResult) {
            is AppResult.Error -> return getCurrentUserResult
            is AppResult.Success -> getCurrentUserResult.data
        }

        if (currentUser.accountStatus !in setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)) {
            return AppResult.Error(UserError.UserForbidden(currentUserId))
        }

        return identifierManager.getUserIdentifierByIdForSelf(identifierId)
            .mapNotNullOrError(
                CommonError.NotFound(
                    resource = UserIdentifier::class.java.simpleName,
                    identifier = identifierId.asHexDashString()
                )
            )
    }
}