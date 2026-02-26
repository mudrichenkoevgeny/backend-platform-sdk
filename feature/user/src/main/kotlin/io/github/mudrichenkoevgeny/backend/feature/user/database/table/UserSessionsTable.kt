package io.github.mudrichenkoevgeny.backend.feature.user.database.table

import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.UserClientType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.javatime.timestamp
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object UserSessionsTable : BaseTable("user_sessions") {
    val userId = reference("user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val userIdentifierId = reference(
        "user_identifier_id",
        UserIdentifiersTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    val userIdentifierAuthProvider = enumerationByName(
        "user_identifier_auth_provider",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        UserAuthProvider::class
    )
    val tokenHash = text("token_hash")
    val expiresAt = timestamp("expires_at")
    val revoked = bool("revoked").default(false)
    val userClientType = enumerationByName(
        "user_client_type",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        UserClientType::class
    ).nullable()
    val userAgent = varchar("user_agent", BaseDbConstraints.DEFAULT_MAX_LENGTH).nullable()
    val ipAddress = varchar("ip_address", BaseDbConstraints.IP_MAX_LENGTH).nullable()
    val language = varchar("language", BaseDbConstraints.LANGUAGE_MAX_LENGTH).nullable()
    val deviceId = text("device_id").nullable()
    val deviceName = text("device_name").nullable()
    val appVersion = varchar("app_version", BaseDbConstraints.VERSION_MAX_LENGTH).nullable()
    val operationSystemVersion = varchar("operation_system_version", BaseDbConstraints.VERSION_MAX_LENGTH).nullable()
    val lastAccessedAt = timestamp("last_accessed_at")
    val lastReauthenticatedAt = timestamp("last_reauthenticated_at")
}