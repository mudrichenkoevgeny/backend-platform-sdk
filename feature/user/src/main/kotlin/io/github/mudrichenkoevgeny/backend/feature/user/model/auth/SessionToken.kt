package io.github.mudrichenkoevgeny.backend.feature.user.model.auth

import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserAuthSpec
import java.time.Instant

/**
 * Pair of access/refresh tokens representing an authenticated session on the client.
 *
 * @property accessToken Short-lived token used to authorize API calls.
 * @property refreshToken Long-lived token used to refresh [accessToken].
 * @property expiresAt Access token expiration instant.
 * @property tokenType Token type for the `Authorization` header (defaults to bearer).
 */
data class SessionToken(
    val accessToken: AccessToken,
    val refreshToken: RefreshToken,
    val expiresAt: Instant,
    val tokenType: String = UserAuthSpec.TOKEN_TYPE_BEARER
)