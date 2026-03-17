package io.github.mudrichenkoevgeny.backend.feature.user.model.session

import io.github.mudrichenkoevgeny.backend.core.common.model.UserDeviceId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserIdentifierId
import io.github.mudrichenkoevgeny.backend.core.common.model.UserSessionId
import io.github.mudrichenkoevgeny.backend.core.common.network.request.model.ClientInfo
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.RefreshTokenHash
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.ExternalAuthProvider
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class UserSessionTest {

    private companion object {
        private const val DEVICE_ID_1 = "device-1"
        private const val DEVICE_ID_2 = "device-2"
        private const val REFRESH_TOKEN_HASH = "hash"
    }

    @Test
    fun `isValid returns false when session is revoked`() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val session = createSession(
            revoked = true,
            expiresAt = now.plusSeconds(60),
            deviceId = UserDeviceId(DEVICE_ID_1)
        )

        assertFalse(session.isValid(clientInfo(UserDeviceId(DEVICE_ID_1)), now))
    }

    @Test
    fun `isValid returns false when session is expired`() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val session = createSession(
            revoked = false,
            expiresAt = now.minusSeconds(1),
            deviceId = UserDeviceId(DEVICE_ID_1)
        )

        assertFalse(session.isValid(clientInfo(UserDeviceId(DEVICE_ID_1)), now))
    }

    @Test
    fun `isValid returns false when device id mismatches`() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val session = createSession(
            revoked = false,
            expiresAt = now.plusSeconds(60),
            deviceId = UserDeviceId(DEVICE_ID_1)
        )

        assertFalse(session.isValid(clientInfo(UserDeviceId(DEVICE_ID_2)), now))
    }

    @Test
    fun `isValid returns true when device id is missing on client`() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val session = createSession(
            revoked = false,
            expiresAt = now.plusSeconds(60),
            deviceId = UserDeviceId(DEVICE_ID_1)
        )

        assertTrue(session.isValid(clientInfo(deviceId = null), now))
    }

    @Test
    fun `isValid returns true when device id is missing in session`() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val session = createSession(
            revoked = false,
            expiresAt = now.plusSeconds(60),
            deviceId = null
        )

        assertTrue(session.isValid(clientInfo(UserDeviceId(DEVICE_ID_1)), now))
    }

    @Test
    fun `isValid returns true when not revoked not expired and device matches`() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val session = createSession(
            revoked = false,
            expiresAt = now.plusSeconds(60),
            deviceId = UserDeviceId(DEVICE_ID_1)
        )

        assertTrue(session.isValid(clientInfo(UserDeviceId(DEVICE_ID_1)), now))
    }

    private fun clientInfo(deviceId: UserDeviceId?): ClientInfo = ClientInfo(
        clientType = null,
        userAgent = null,
        ipAddress = null,
        language = null,
        host = null,
        origin = null,
        deviceId = deviceId,
        deviceName = null,
        appVersion = null,
        operationSystemVersion = null
    )

    private fun createSession(
        revoked: Boolean,
        expiresAt: Instant,
        deviceId: UserDeviceId?
    ): UserSession {
        return UserSession(
            id = UserSessionId.generate(),
            userId = UserId.generate(),
            userIdentifierId = UserIdentifierId.generate(),
            userIdentifierAuthProvider = ExternalAuthProvider.Google.userAuthProvider,
            refreshTokenHash = RefreshTokenHash(REFRESH_TOKEN_HASH),
            expiresAt = expiresAt,
            revoked = revoked,
            userClientType = null,
            userAgent = null,
            ipAddress = null,
            language = null,
            userDeviceId = deviceId,
            userDeviceName = null,
            appVersion = null,
            operationSystemVersion = null,
            createdAt = Instant.EPOCH,
            updatedAt = null,
            lastAccessedAt = Instant.EPOCH,
            lastReauthenticatedAt = Instant.EPOCH
        )
    }
}

