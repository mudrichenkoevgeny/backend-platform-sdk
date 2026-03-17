package io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshToken
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshTokenHash

/**
 * Generates opaque refresh tokens and computes their secure hash representation.
 *
 * The raw refresh token should only be sent to the client. Server-side storage and comparisons
 * must use [RefreshTokenHash].
 */
interface RefreshTokenProvider {
    /** Generates a new refresh token value. */
    fun getRefreshToken(): AppResult<RefreshToken>
    /** Computes a one-way hash for [refreshToken] suitable for persistence. */
    fun getRefreshTokenHash(refreshToken: RefreshToken): AppResult<RefreshTokenHash>
}