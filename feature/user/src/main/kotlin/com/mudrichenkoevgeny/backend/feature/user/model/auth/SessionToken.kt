package io.github.mudrichenkoevgeny.backend.feature.user.model.auth

import io.github.mudrichenkoevgeny.backend.feature.user.network.constants.UserNetworkConstants
import java.time.Instant

data class SessionToken(
    val accessToken: AccessToken,
    val refreshToken: RefreshToken,
    val expiresAt: Instant,
    val tokenType: String = UserNetworkConstants.AUTHORIZATION_HEADER_BEARER
)