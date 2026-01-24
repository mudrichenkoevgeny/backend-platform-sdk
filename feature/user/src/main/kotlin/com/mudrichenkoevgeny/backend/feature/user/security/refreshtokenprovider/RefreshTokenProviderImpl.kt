package com.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider

import com.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshToken
import com.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshTokenHash
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RefreshTokenProviderImpl @Inject constructor(): RefreshTokenProvider {
    override fun getRefreshToken(): AppResult<RefreshToken> {
        return AppResult.Success(
            RefreshToken(UUID.randomUUID().toString() + "." + UUID.randomUUID().toString())
        )
    }

    override fun getRefreshTokenHash(refreshToken: RefreshToken): AppResult<RefreshTokenHash> {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(refreshToken.value.toByteArray())
            AppResult.Success(RefreshTokenHash(Base64.getEncoder().encodeToString(hash)))
        } catch (t: Throwable) {
            AppResult.Error(CommonError.System(t))
        }
    }
}