package com.mudrichenkoevgeny.backend.feature.user.database.table

import com.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import com.mudrichenkoevgeny.backend.core.database.table.BaseTable
import com.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp

object UserSessionsTable : BaseTable("user_refresh_tokens") {
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
    val userAgent = varchar("user_agent", BaseDbConstraints.DEFAULT_MAX_LENGTH).nullable()
    val ipAddress = varchar("ip_address", BaseDbConstraints.IP_MAX_LENGTH).nullable()
    val deviceId = text("device_id").nullable()
    val deviceName = text("device_name").nullable()
    val lastAccessedAt = timestamp("last_accessed_at")
    val lastReauthenticatedAt = timestamp("last_reauthenticated_at")

    init {
        index("idx_tokens_hash", false, tokenHash)
        index("idx_tokens_user_id", false, userId)
    }
}