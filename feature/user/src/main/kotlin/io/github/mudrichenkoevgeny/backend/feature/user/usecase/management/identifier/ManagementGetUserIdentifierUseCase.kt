package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.identifier

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagementGetUserIdentifierUseCase @Inject constructor(
    private val userManager: UserManager,
    private val identifierManager: IdentifierManager
) {
    suspend operator fun invoke(
        userId: UserId,
        userIdentifierId: UserIdentifierId,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<UserIdentifierInternal> {
        val currentUserId = authenticatedRequestContext.userId

        val getCurrentUserResult = userManager.getUserById(currentUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val currentUser = when (getCurrentUserResult) {
            is AppResult.Error -> return getCurrentUserResult
            is AppResult.Success -> getCurrentUserResult.data
        }

        return identifierManager.getUserIdentifierById(
            userIdentifierId = userIdentifierId,
            userId = currentUserId,
            userPermissionCodes = currentUser.permissions
        ).mapNotNullOrError(
            CommonError.NotFound(
                resource = UserIdentifierInternal::class.java.simpleName,
                identifier = userIdentifierId.asHexDashString()
            )
        )
    }
}
