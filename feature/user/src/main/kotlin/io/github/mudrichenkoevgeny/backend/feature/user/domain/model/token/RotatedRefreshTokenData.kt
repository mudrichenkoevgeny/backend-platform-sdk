package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.token

import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.toUserIdentifierIdOrThrow
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.session.toUserSessionIdOrThrow
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.AccessToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.RefreshToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.token.SessionToken
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.toUserIdOrThrow
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class RotatedRefreshTokenData(
    val sessionId: String,
    val identifierId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant,
    val userId: String,
    val rotatedAt: Instant
) {
    fun toSessionToken(): SessionToken = SessionToken(
        sessionId = sessionId.toUserSessionIdOrThrow(),
        identifierId = identifierId.toUserIdentifierIdOrThrow(),
        accessToken = AccessToken(accessToken),
        refreshToken = RefreshToken(refreshToken),
        expiresAt = expiresAt
    )

    fun getUserId(): UserId = userId.toUserIdOrThrow()

    companion object {
        fun from(sessionToken: SessionToken, userId: UserId, rotatedAt: Instant): RotatedRefreshTokenData =
            RotatedRefreshTokenData(
                sessionId = sessionToken.sessionId.asHexDashString(),
                identifierId = sessionToken.identifierId.asHexDashString(),
                accessToken = sessionToken.accessToken.value,
                refreshToken = sessionToken.refreshToken.value,
                expiresAt = sessionToken.expiresAt,
                userId = userId.asHexDashString(),
                rotatedAt = rotatedAt
            )
    }
}
