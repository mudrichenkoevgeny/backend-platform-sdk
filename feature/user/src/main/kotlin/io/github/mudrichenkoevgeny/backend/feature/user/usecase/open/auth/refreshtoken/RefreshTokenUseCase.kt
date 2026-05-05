package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.refreshtoken

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider.RefreshTokenProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.SessionToken
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefreshTokenUseCase @Inject constructor(
    private val sessionManager: SessionManager,
    private val rateLimiter: RateLimiter,
    private val refreshTokenProvider: RefreshTokenProvider
) {
    /**
     * Exchanges a valid refresh token for a new pair of session tokens (Access and Refresh).
     *
     * **Allowed Account Statuses:** Any (Handled by session validation logic).
     *
     * **Security:**
     * - Uses a cryptographic hash of the refresh token as a rate limit identifier to prevent brute-force
     *   probing of token values without exposing the raw token in logs or memory.
     * - Enforces rate limiting via [UserRateLimitAction.REFRESH_TOKEN].
     *
     * **Workflow:**
     * 1. Computes the hash of the provided [refreshToken] via [RefreshTokenProvider].
     * 2. Checks rate limits using the resulting hash as the identifier.
     * 3. Delegates the token rotation and session update logic to [SessionManager].
     *
     * @param refreshToken The current refresh token to be rotated.
     * @param authenticatedRequestContext The context of the request, including user identity and client info.
     * @return [AppResult] containing the new [SessionToken] upon success.
     */
    suspend operator fun invoke(
        refreshToken: RefreshToken,
        authenticatedRequestContext: AuthenticatedRequestContext
    ): AppResult<SessionToken> {
        val refreshTokenHashResult = refreshTokenProvider.getRefreshTokenHash(refreshToken)
        val refreshTokenHash = when (refreshTokenHashResult) {
            is AppResult.Success -> refreshTokenHashResult.data
            is AppResult.Error -> return refreshTokenHashResult
        }

        val rateLimitCheck = rateLimiter.checkRateLimit(
            action = UserRateLimitAction.REFRESH_TOKEN,
            identifier = refreshTokenHash.value
        )

        if (rateLimitCheck is AppResult.Error) {
            return rateLimitCheck
        }

        return sessionManager.refreshSession(
            userId = authenticatedRequestContext.userId,
            refreshToken = refreshToken,
            clientInfo = authenticatedRequestContext.clientInfo
        )
    }
}
