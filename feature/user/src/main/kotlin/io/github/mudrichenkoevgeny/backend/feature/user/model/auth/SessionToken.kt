package io.github.mudrichenkoevgeny.backend.feature.user.model.auth

import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.constants.UserNetworkFoundationConstants
import java.time.Instant

data class SessionToken(
    val accessToken: AccessToken,
    val refreshToken: RefreshToken,
    val expiresAt: Instant,
    val tokenType: String = UserNetworkFoundationConstants.AUTHORIZATION_HEADER_BEARER
)