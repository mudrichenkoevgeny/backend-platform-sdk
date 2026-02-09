package io.github.mudrichenkoevgeny.backend.feature.user.model.auth

import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserAuthSpec
import java.time.Instant

data class SessionToken(
    val accessToken: AccessToken,
    val refreshToken: RefreshToken,
    val expiresAt: Instant,
    val tokenType: String = UserAuthSpec.TOKEN_TYPE_BEARER
)