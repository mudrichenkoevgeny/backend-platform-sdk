package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.session

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
class GetSessionUseCase @Inject constructor(
    private val userManager: UserManager,
    private val sessionManager: SessionManager
) {
    /**
     * Retrieves specific session details by its identifier for the current authenticated user.
     *
     * **Allowed Account Statuses:** Any.
     *
     * **Workflow:**
     * 1. Validates the existence and accessibility of the user via [UserManager].
     * 2. Fetches session details from [SessionManager] using the provided [sessionId].
     * 3. Ensures the session exists and belongs to the caller.
     *
     * @param sessionId The unique identifier of the requested session.
     * @param authenticatedRequestContext The context of the authenticated request.
     * @return [AppResult] containing the [UserSession] or a [CommonError.NotFound] error.
     */
    suspend operator fun invoke(
        sessionId: UserSessionId,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<UserSession> {
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

        return sessionManager.getUserSessionForSelf(
            userSessionId = sessionId
        ).mapNotNullOrError(
            CommonError.NotFound(
                resource = UserSession::class.java.simpleName,
                identifier = sessionId.asHexDashString()
            )
        )
    }
}