package com.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider

import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshToken
import com.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshTokenHash

interface RefreshTokenProvider {
    fun getRefreshToken(): AppResult<RefreshToken>
    fun getRefreshTokenHash(refreshToken: RefreshToken): AppResult<RefreshTokenHash>
}