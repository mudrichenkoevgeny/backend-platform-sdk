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

/**
 * Represents cached metadata for a previously rotated refresh token.
 *
 * Used during refresh token rotation with a grace period to support client retries and
 * detect replay attacks. If an old refresh token is presented within the grace window,
 * the corresponding new [SessionToken] is returned without generating a fresh session.
 * If presented outside the grace window, security measures (such as account security hold) are triggered.
 *
 * @property sessionId The unique session identifier string.
 * @property identifierId The unique identifier record ID string.
 * @property accessToken The access token string issued during rotation.
 * @property refreshToken The new refresh token string issued during rotation.
 * @property expiresAt Expiration timestamp of the access token.
 * @property userId The unique user identifier string.
 * @property rotatedAt Timestamp when the refresh token rotation occurred.
 */
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
    /**
     * Converts this rotated data back into a [SessionToken].
     */
    fun toSessionToken(): SessionToken = SessionToken(
        sessionId = sessionId.toUserSessionIdOrThrow(),
        identifierId = identifierId.toUserIdentifierIdOrThrow(),
        accessToken = AccessToken(accessToken),
        refreshToken = RefreshToken(refreshToken),
        expiresAt = expiresAt
    )

    /**
     * Returns the [UserId] associated with this token rotation.
     */
    fun getUserId(): UserId = userId.toUserIdOrThrow()

    companion object {
        /**
         * Creates a [RotatedRefreshTokenData] instance from a [SessionToken], [UserId], and rotation timestamp.
         */
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
