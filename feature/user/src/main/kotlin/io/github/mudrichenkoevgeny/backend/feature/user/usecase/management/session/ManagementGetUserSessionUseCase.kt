package io.github.mudrichenkoevgeny.backend.feature.user.usecase.management.session

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagementGetUserSessionUseCase @Inject constructor(
    private val userManager: UserManager,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(
        userId: UserId,
        userSessionId: UserSessionId,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<UserSessionInternal> {
        val currentUserId = authenticatedRequestContext.userId

        val getCurrentUserResult = userManager.getUserById(currentUserId)
            .mapNotNullOrError(UserError.UserForbidden())

        val currentUser = when (getCurrentUserResult) {
            is AppResult.Error -> return getCurrentUserResult
            is AppResult.Success -> getCurrentUserResult.data
        }

        val getSessionResult = sessionManager.getUserSessionById(
            userSessionId = userSessionId,
            userId = currentUserId,
            userPermissionCodes = currentUser.permissions
        )

        val scopedSessionResult = when (getSessionResult) {
            is AppResult.Error -> getSessionResult
            is AppResult.Success -> {
                val session = getSessionResult.data
                if (session != null && session.userId != userId) AppResult.Success(null) else getSessionResult
            }
        }

        return scopedSessionResult.mapNotNullOrError(
            CommonError.NotFound(
                resource = UserSessionInternal::class.java.simpleName,
                identifier = userSessionId.asHexDashString()
            )
        )
    }
}
