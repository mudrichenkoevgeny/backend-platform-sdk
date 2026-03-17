package io.github.mudrichenkoevgeny.backend.feature.user.model.session

import io.github.mudrichenkoevgeny.backend.core.common.model.UserDeviceId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserSessionId
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshTokenHash
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.UserClientType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import java.time.Instant

/**
 * Persisted authenticated session.
 *
 * Stores session metadata (client/device info, expiry, revocation flag) and provides
 * a small validity check used by auth flows.
 */
data class UserSession(
    val id: UserSessionId,
    val userId: UserId,
    val userIdentifierId: UserIdentifierId,
    val userIdentifierAuthProvider: UserAuthProvider,
    val refreshTokenHash: RefreshTokenHash,
    val expiresAt: Instant,
    val revoked: Boolean,
    val userClientType: UserClientType?,
    val userAgent: String?,
    val ipAddress: String?,
    val language: String?,
    val userDeviceId: UserDeviceId?,
    val userDeviceName: String?,
    val appVersion: String?,
    val operationSystemVersion: String?,
    val createdAt: Instant,
    val updatedAt: Instant?,
    val lastAccessedAt: Instant,
    val lastReauthenticatedAt: Instant
) {

    /**
     * Returns `true` when the session is not revoked, not expired and matches the caller device.
     *
     * Device matching is permissive: if either the session or the client does not provide a device id,
     * the session is considered valid for device checks.
     */
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