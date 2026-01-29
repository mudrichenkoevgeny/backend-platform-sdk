package io.github.mudrichenkoevgeny.backend.feature.user.security.tokenprovider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AccessToken
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshToken
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshTokenHash
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserSessionId
import java.time.Instant

interface TokenProvider {
    fun generateAccessToken(
        userId: UserId,
        sessionId: UserSessionId,
        issuedAt: Instant,
        expiration: Instant
    ): AppResult<AccessToken>
    fun verifyAccessToken(accessToken: AccessToken): AppResult<UserId>
    fun generateRefreshToken(): AppResult<RefreshToken>
    fun getRefreshTokenHash(refreshToken: RefreshToken): AppResult<RefreshTokenHash>
}
