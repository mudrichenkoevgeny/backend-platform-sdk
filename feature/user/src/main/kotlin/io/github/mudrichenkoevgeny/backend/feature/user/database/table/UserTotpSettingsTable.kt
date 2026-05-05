package io.github.mudrichenkoevgeny.backend.feature.user.database.table

import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import kotlinx.serialization.serializer
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.json.jsonb

/**
 * Table for storing TOTP (Time-based One-Time Password) security settings.
 * * Linked to [UsersTable] via [userId]. Contains encrypted secrets and hashed recovery codes.
 */
object UserTotpSettingsTable : BaseTable("user_totp_settings") {
    val userId = reference("user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE).uniqueIndex()
    val encryptedSecret = varchar("encrypted_secret", BaseDbConstraints.DEFAULT_MAX_LENGTH)
    val isConfirmed = bool("is_confirmed").default(false)
    val encryptedRecoveryCodes = jsonb<List<String>>(
        "encrypted_recovery_codes",
        FoundationJson,
        serializer<List<String>>()
    ).nullable()
    val lastUsedAt = timestamp("last_used_at").nullable()
}