package io.github.mudrichenkoevgeny.backend.feature.user.database.table

import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.javatime.timestamp

/**
 * User sessions table storing refresh sessions and client metadata.
 *
 * Schema is created by a Flyway migration in `db/migration/feature/user/`.
 * The app must include this path in its Flyway migration locations.
 */
object UserSessionsTable : BaseTable("user_sessions") {
    val userId = reference("user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val userRole = enumerationByName(
        "user_role",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        UserRole::class
    )
    val identifier = text("identifier")
    val identifierId = reference(
        "identifier_id",
        UserIdentifiersTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    val identifierAuthProvider = enumerationByName(
        "identifier_auth_provider",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        UserAuthProvider::class
    )
    val refreshTokenHash = text("refresh_token_hash").uniqueIndex()

    // Device Info
    val clientType = enumerationByName(
        "client_type",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        ClientType::class
    ).nullable()
    val deviceId = text("device_id").nullable()
    val deviceName = text("device_name").nullable()
    val appVersion = varchar("app_version", BaseDbConstraints.VERSION_MAX_LENGTH).nullable()
    val operationSystemVersion = varchar("operation_system_version", BaseDbConstraints.VERSION_MAX_LENGTH).nullable()
    val language = varchar("language", BaseDbConstraints.LANGUAGE_MAX_LENGTH).nullable()

    val userAgent = varchar("user_agent", BaseDbConstraints.DEFAULT_MAX_LENGTH).nullable()
    val ipAddress = varchar("ip_address", BaseDbConstraints.IP_MAX_LENGTH).nullable()

    val expiresAt = timestamp("expires_at")
    val lastAccessedAt = timestamp("last_accessed_at")
    val lastReauthenticatedAt = timestamp("last_reauthenticated_at")
}