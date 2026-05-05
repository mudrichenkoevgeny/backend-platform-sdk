package io.github.mudrichenkoevgeny.backend.feature.user.service.authenticationchallenge

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionInternal
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails

/**
 * Service for enforcing step-up authentication policies.
 * Verifies if the session security level is sufficient for sensitive operations
 * based on the time of the last successful MFA re-authentication.
 */
interface AuthenticationChallengeService {

    /**
     * Verifies that the session has a fresh multifactor confirmation.
     * * If the session is considered "stale" based on [UserDetails.role] policies,
     * it initiates a new MFA challenge.
     *
     * @param userDetails The domain model of the user performing the action.
     * @param userSession The current active session to be verified.
     * @return [AppResult.Success] if the session is still within the validity window,
     * or [AppResult.Error] (typically [SecurityError.TotpConfirmationRequired]) if a step-up is required.
     */
    suspend fun ensureSessionConfirmed(
        userDetails: UserDetails,
        userSession: UserSessionInternal
    ): AppResult<Unit>
}