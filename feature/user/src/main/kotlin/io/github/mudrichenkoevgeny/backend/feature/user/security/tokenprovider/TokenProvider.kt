package io.github.mudrichenkoevgeny.backend.feature.user.security.tokenprovider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.UserSessionId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.AccessToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshTokenHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import java.time.Instant

/**
 * Issues and verifies authentication tokens for the user feature.
 *
 * Implementations typically generate:
 * - short-lived access tokens used for request authentication,
 * - long-lived refresh tokens used to renew access without re-authentication.
 *
 * The provider also exposes a hashing function to store/compare refresh tokens securely.
 */
interface TokenProvider {
    /**
     * Creates a signed access token for the given user and session.
     *
     * @param userId authenticated user id
     * @param userRole [UserRole] to embed in the token claims for downstream authorization.
     * @param sessionId authenticated user session id
     * @param issuedAt token issuance time
     * @param expiration token expiration time
     * @return [AppResult.Success] with an [AccessToken], or [AppResult.Error] on failure
     */
    fun generateAccessToken(
        userId: UserId,
        userRole: UserRole,
        sessionId: UserSessionId,
        issuedAt: Instant,
        expiration: Instant
    ): AppResult<AccessToken>

    /**
     * Verifies [accessToken] and extracts the authenticated [UserId].
     *
     * @return [AppResult.Success] with the user id, or [AppResult.Error] when token is invalid/expired
     */
    fun verifyAccessToken(accessToken: AccessToken): AppResult<UserId>

    /**
     * Generates a new opaque refresh token.
     */
    fun generateRefreshToken(): AppResult<RefreshToken>

    /**
     * Computes a one-way hash for secure storage/comparison of [refreshToken].
     */
    fun getRefreshTokenHash(refreshToken: RefreshToken): AppResult<RefreshTokenHash>
}
