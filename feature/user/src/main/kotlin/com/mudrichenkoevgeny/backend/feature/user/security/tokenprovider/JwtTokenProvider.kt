package com.mudrichenkoevgeny.backend.feature.user.security.tokenprovider

import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import com.mudrichenkoevgeny.backend.feature.user.model.auth.AccessToken
import com.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshToken
import com.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshTokenHash
import com.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import com.mudrichenkoevgeny.backend.core.common.model.UserId
import com.mudrichenkoevgeny.backend.core.common.model.UserSessionId
import com.mudrichenkoevgeny.backend.feature.user.security.jwt.getUserIdFromPayload
import com.mudrichenkoevgeny.backend.feature.user.security.jwt.withSessionIdSubject
import com.mudrichenkoevgeny.backend.feature.user.security.jwt.withUserIdSubject
import com.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider.RefreshTokenProvider
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.time.Instant
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JwtTokenProvider @Inject constructor(
    userConfig: UserConfig,
    private val refreshTokenProvider: RefreshTokenProvider
) : TokenProvider {

    private val key = Keys.hmacShaKeyFor(userConfig.jwtSecret.toByteArray())

    override fun generateAccessToken(
        userId: UserId,
        sessionId: UserSessionId,
        issuedAt: Instant,
        expiration: Instant
    ): AppResult<AccessToken> {
        return try {
            val accessToken = Jwts.builder()
                .withUserIdSubject(userId)
                .withSessionIdSubject(sessionId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact()

            AppResult.Success(AccessToken(accessToken))
        } catch (_: Exception) {
            AppResult.Error(UserError.InvalidAccessToken())
        }
    }

    override fun verifyAccessToken(accessToken: AccessToken): AppResult<UserId> {
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(accessToken.value)

            claims.getUserIdFromPayload()
        } catch (_: ExpiredJwtException) {
            AppResult.Error(UserError.AccessTokenExpired())
        } catch (_: Exception) {
            AppResult.Error(UserError.InvalidAccessToken())
        }
    }

    override fun generateRefreshToken(): AppResult<RefreshToken> {
        return refreshTokenProvider.getRefreshToken()
    }

    override fun getRefreshTokenHash(refreshToken: RefreshToken): AppResult<RefreshTokenHash> {
        return refreshTokenProvider.getRefreshTokenHash(refreshToken)
    }
}
