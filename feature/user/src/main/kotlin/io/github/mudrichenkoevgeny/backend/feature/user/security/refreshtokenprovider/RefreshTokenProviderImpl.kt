package io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshTokenHash
import java.security.MessageDigest
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.Uuid

@Singleton
/**
 * Default [RefreshTokenProvider] implementation.
 *
 * Token format:
 * - two random UUIDs separated by a dot (`.`) to keep it opaque and hard to guess.
 *
 * Hashing:
 * - SHA-256 digest encoded as Base64 to make storage and transmission safe.
 */
class RefreshTokenProviderImpl @Inject constructor() : RefreshTokenProvider {
    override fun getRefreshToken(): AppResult<RefreshToken> {
        return AppResult.Success(
            RefreshToken("${Uuid.random()}.${Uuid.random()}")
        )
    }

    override fun getRefreshTokenHash(refreshToken: RefreshToken): AppResult<RefreshTokenHash> {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(refreshToken.value.toByteArray())
            AppResult.Success(RefreshTokenHash(Base64.getEncoder().encodeToString(hash)))
        } catch (t: Throwable) {
            AppResult.Error(CommonError.Internal(t))
        }
    }
}