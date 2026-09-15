package io.github.mudrichenkoevgeny.backend.feature.user.model.device

import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import kotlin.time.Instant

/**
 * Represents a device that a user has successfully authenticated from.
 *
 * Used for detecting new device logins and triggering security alerts.
 *
 * @property userId unique identifier of the user who owns this device record
 * @property deviceId unique device identifier
 * @property firstIpAddress IP address used during the initial authentication, or `null` if unavailable
 * @property lastIpAddress most recent IP address observed for this device, or `null` if unavailable
 * @property userAgent HTTP User-Agent string from the client, or `null` if unavailable
 * @property firstSeenAt timestamp of the initial authentication from this device
 * @property lastSeenAt timestamp of the most recent authentication or activity from this device
 */
data class UserKnownDevice(
    val userId: UserId,
    val deviceId: String,
    val firstIpAddress: String?,
    val lastIpAddress: String?,
    val userAgent: String?,
    val firstSeenAt: Instant,
    val lastSeenAt: Instant
)