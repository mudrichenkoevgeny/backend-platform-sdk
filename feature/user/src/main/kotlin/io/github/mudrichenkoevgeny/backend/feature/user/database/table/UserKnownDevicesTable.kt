package io.github.mudrichenkoevgeny.backend.feature.user.database.table

import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.javatime.timestamp

/**
 * Tracks known devices for users to detect logins from new devices.
 *
 * Schema is created by a Flyway migration in `db/migration/feature/user/`.
 * The app must include this path in its Flyway migration locations.
 */
object UserKnownDevicesTable : BaseTable("user_known_devices") {
    val userId = reference("user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val deviceId = text("device_id")
    val firstIpAddress = varchar("first_ip_address", BaseDbConstraints.IP_MAX_LENGTH).nullable()
    val lastIpAddress = varchar("last_ip_address", BaseDbConstraints.IP_MAX_LENGTH).nullable()
    val userAgent = varchar("user_agent", BaseDbConstraints.DEFAULT_MAX_LENGTH).nullable()
    val firstSeenAt = timestamp("first_seen_at")
    val lastSeenAt = timestamp("last_seen_at")
}
