package io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.auth.refreshtoken

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.RateLimiter
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.RequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.ratelimiter.model.UserRateLimitAction
import io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider.RefreshTokenProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.SessionToken
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case: exchange a refresh token for a new session token.
 *
 * Rate limit uses the same identifier as session storage: [RefreshTokenProvider.getRefreshTokenHash]
 * (see [RefreshTokenProvider]). Then delegates to [SessionManager.refreshSession].
 *
 * Returns [AppResult.Success] with [SessionToken], or [AppResult.Error] (e.g. invalid refresh,
 * `CommonError.TooManyRequests`, or errors from hashing / persistence).
 */
@Singleton
class RefreshTokenUseCase @Inject constructor(
    private val sessionManager: SessionManager,
    private val rateLimiter: RateLimiter,
    private val refreshTokenProvider: RefreshTokenProvider
) {
    suspend operator fun invoke(
        refreshToken: RefreshToken,
        requestContext: RequestContext
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
            userId = requestContext.userId,
            refreshToken = refreshToken,
            clientInfo = requestContext.clientInfo
        )
    }
}
