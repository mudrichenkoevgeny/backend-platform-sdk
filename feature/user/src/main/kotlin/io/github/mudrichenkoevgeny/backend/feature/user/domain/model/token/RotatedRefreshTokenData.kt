package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.token

import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.AccessToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.SessionToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class RotatedRefreshTokenData(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant,
    val userId: String,
    val rotatedAt: Instant
) {
    fun toSessionToken(): SessionToken = SessionToken(
        accessToken = AccessToken(accessToken),
        refreshToken = RefreshToken(refreshToken),
        expiresAt = expiresAt
    )

    fun getUserId(): UserId = UserId(Uuid.parse(userId))

    companion object {
        fun from(sessionToken: SessionToken, userId: UserId, rotatedAt: Instant): RotatedRefreshTokenData =
            RotatedRefreshTokenData(
                accessToken = sessionToken.accessToken.value,
                refreshToken = sessionToken.refreshToken.value,
                expiresAt = sessionToken.expiresAt,
                userId = userId.asHexDashString(),
                rotatedAt = rotatedAt
            )
    }
}
