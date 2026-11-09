package io.github.mudrichenkoevgeny.backend.feature.user.database.table

import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * Tracks known devices for users to detect logins from new devices.
 *
 * Schema is created by a Flyway migration in `db/migration/feature/user/`.
 * The app must include this path in its Flyway migration locations.
 */
public object UserKnownDevicesTable : BaseTable("user_known_devices") {
    public val userId: Column<EntityID<Uuid>> = reference(
        "user_id",
        UsersTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    public val deviceId: Column<Uuid> = uuid("device_id")
    public val firstIpAddress: Column<String?> = varchar(
        "first_ip_address",
        BaseDbConstraints.IP_MAX_LENGTH
    ).nullable()
    public val lastIpAddress: Column<String?> = varchar(
        "last_ip_address",
        BaseDbConstraints.IP_MAX_LENGTH
    ).nullable()
    public val userAgent: Column<String?> = varchar(
        "user_agent",
        BaseDbConstraints.DEFAULT_MAX_LENGTH
    ).nullable()
    public val firstSeenAt: Column<Instant> = timestamp("first_seen_at")
    public val lastSeenAt: Column<Instant> = timestamp("last_seen_at")
}
