package io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshToken
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshTokenHash

interface RefreshTokenProvider {
    fun getRefreshToken(): AppResult<RefreshToken>
    fun getRefreshTokenHash(refreshToken: RefreshToken): AppResult<RefreshTokenHash>
}