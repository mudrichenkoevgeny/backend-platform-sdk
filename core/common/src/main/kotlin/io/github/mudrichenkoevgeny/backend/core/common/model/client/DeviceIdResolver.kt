package io.github.mudrichenkoevgeny.backend.core.common.model.client

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientDeviceId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientInfo
import java.security.MessageDigest
import kotlin.uuid.Uuid

/**
 * Utility for resolving client device identifiers.
 *
 * Uses the client's explicitly provided [ClientDeviceId] if present in [ClientInfo],
 * or generates a deterministic fallback identifier derived from user ID, client IP, and user-agent.
 */
object DeviceIdResolver {

    /**
     * Resolves the [ClientDeviceId] for a given user and request context.
     *
     * @param userId user identifier string.
     * @param clientInfo client request metadata containing device, IP, and user-agent information.
     * @return explicit or deterministic fallback [ClientDeviceId].
     */
    fun resolveDeviceId(userId: String, clientInfo: ClientInfo): ClientDeviceId {
        val clientDeviceId = clientInfo.clientDeviceInfo.deviceId
        if (clientDeviceId != null) {
            return clientDeviceId
        }

        val ip = clientInfo.ipAddress ?: "unknown_ip"
        val userAgent = clientInfo.userAgent ?: "unknown_ua"
        val rawString = "$userId:$ip:$userAgent"
        val hashBytes = MessageDigest.getInstance("SHA-256").digest(rawString.toByteArray())
        val mostSigBits = hashBytes.toLong(0)
        val leastSigBits = hashBytes.toLong(8)
        return ClientDeviceId(Uuid.fromLongs(mostSigBits, leastSigBits))
    }

    private fun ByteArray.toLong(offset: Int): Long {
        var result = 0L
        for (i in 0..7) {
            result = (result shl 8) or (this[offset + i].toLong() and 0xFF)
        }
        return result
    }
}
