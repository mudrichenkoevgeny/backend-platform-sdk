package com.mudrichenkoevgeny.backend.feature.user.security.tokenprovider

import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.feature.user.model.auth.AccessToken
import com.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshToken
import com.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshTokenHash
import com.mudrichenkoevgeny.backend.core.common.model.UserId
import com.mudrichenkoevgeny.backend.core.common.model.UserSessionId
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
