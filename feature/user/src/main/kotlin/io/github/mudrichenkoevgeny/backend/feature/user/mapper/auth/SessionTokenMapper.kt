package io.github.mudrichenkoevgeny.backend.feature.user.mapper.auth

import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.SessionToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.response.token.SessionTokenResponse

/**
 * Maps internal [SessionToken] to the shared network response contract.
 */
fun SessionToken.toSessionTokenResponse(): SessionTokenResponse = SessionTokenResponse(
    accessToken = accessToken.value,
    refreshToken = refreshToken.value,
    expiresAt = expiresAt.toEpochMilli(),
    tokenType = tokenType
)