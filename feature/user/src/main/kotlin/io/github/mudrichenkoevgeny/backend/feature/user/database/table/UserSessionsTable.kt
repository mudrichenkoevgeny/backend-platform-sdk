package io.github.mudrichenkoevgeny.backend.feature.user.database.table

import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * User sessions table storing refresh sessions and client metadata.
 *
 * Schema is created by a Flyway migration in `db/migration/feature/user/`.
 * The app must include this path in its Flyway migration locations.
 */
public object UserSessionsTable : BaseTable("user_sessions") {
    public val userId: Column<EntityID<Uuid>> = reference(
        "user_id",
        UsersTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    public val userRole: Column<UserRole> = enumerationByName(
        "user_role",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        UserRole::class
    )
    public val identifier: Column<String> = text("identifier")
    public val identifierDisplayName: Column<String> = text("identifier_display_name")
    public val identifierId: Column<EntityID<Uuid>> = reference(
        "identifier_id",
        UserIdentifiersTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    public val identifierAuthProvider: Column<UserAuthProvider> = enumerationByName(
        "identifier_auth_provider",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        UserAuthProvider::class
    )
    public val refreshTokenHash: Column<String> = text("refresh_token_hash").uniqueIndex()

    public val clientType: Column<ClientType?> = enumerationByName(
        "client_type",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        ClientType::class
    ).nullable()
    public val deviceId: Column<Uuid?> = uuid("device_id").nullable()
    public val deviceName: Column<String?> = text("device_name").nullable()
    public val appVersion: Column<String?> = varchar(
        "app_version",
        BaseDbConstraints.VERSION_MAX_LENGTH
    ).nullable()
    public val operationSystemVersion: Column<String?> = varchar(
        "operation_system_version",
        BaseDbConstraints.VERSION_MAX_LENGTH
    ).nullable()
    public val language: Column<String?> = varchar(
        "language",
        BaseDbConstraints.LANGUAGE_MAX_LENGTH
    ).nullable()

    public val userAgent: Column<String?> = varchar(
        "user_agent",
        BaseDbConstraints.DEFAULT_MAX_LENGTH
    ).nullable()
    public val ipAddress: Column<String?> = varchar(
        "ip_address",
        BaseDbConstraints.IP_MAX_LENGTH
    ).nullable()

    public val expiresAt: Column<Instant> = timestamp("expires_at")
    public val lastAccessedAt: Column<Instant> = timestamp("last_accessed_at")
    public val lastReauthenticatedAt: Column<Instant> = timestamp("last_reauthenticated_at")
}
