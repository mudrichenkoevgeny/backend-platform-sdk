package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetUserUseCase @Inject constructor(
    private val userManager: UserManager
) {
    /**
     * Retrieves the profile details of the currently authenticated user.
     *
     * **Allowed Account Statuses:** Any.
     *
     * **Workflow:**
     * 1. Extracts the userId from the authenticated request context.
     * 2. Fetches user details via [UserManager].
     *
     * @param authenticatedRequestContext The context of the authenticated request.
     * @return [AppResult] containing [UserDetails].
     */
    suspend operator fun invoke(
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<UserDetails> {
        val currentUserId = authenticatedRequestContext.userId

        return userManager.getUserByIdForSelf(currentUserId).mapNotNullOrError(UserError.UserForbidden())
    }
}