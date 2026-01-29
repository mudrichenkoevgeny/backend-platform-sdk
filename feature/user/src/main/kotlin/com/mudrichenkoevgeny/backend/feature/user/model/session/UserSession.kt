package io.github.mudrichenkoevgeny.backend.feature.user.model.session

import io.github.mudrichenkoevgeny.backend.core.common.model.UserDeviceId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserSessionId
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshTokenHash
import java.time.Instant

data class UserSession(
    val id: UserSessionId,
    val userId: UserId,
    val userIdentifierId: UserIdentifierId,
    val userIdentifierAuthProvider: UserAuthProvider,
    val refreshTokenHash: RefreshTokenHash,
    val expiresAt: Instant,
    val revoked: Boolean,
    val userAgent: String? = null,
    val ipAddress: String? = null,
    val userDeviceId: UserDeviceId?,
    val userDeviceName: String?,
    val createdAt: Instant,
    val updatedAt: Instant?,
    val lastAccessedAt: Instant,
    val lastReauthenticatedAt: Instant
) {

    fun isValid(clientInfo: ClientInfo, now: Instant): Boolean {
        return !revoked
                && !isExpired(now)
                && isCorrectDevice(clientInfo.deviceId)
    }

    private fun isExpired(now: Instant): Boolean = expiresAt.isBefore(now)

    private fun isCorrectDevice(clientDeviceId: UserDeviceId?): Boolean {
        val sessionDeviceId = userDeviceId
        if (clientDeviceId == null || sessionDeviceId == null) {
            return true
        }
        return clientDeviceId == sessionDeviceId
    }
}