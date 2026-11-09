package io.github.mudrichenkoevgeny.backend.feature.user.database.table

import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import kotlinx.serialization.serializer
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * Table for storing TOTP (Time-based One-Time Password) security settings.
 * * Linked to [UsersTable] via [userId]. Contains encrypted secrets and hashed recovery codes.
 */
public object UserTotpSettingsTable : BaseTable("user_totp_settings") {
    public val userId: Column<EntityID<Uuid>> = reference(
        "user_id",
        UsersTable.id,
        onDelete = ReferenceOption.CASCADE
    ).uniqueIndex()
    public val encryptedSecret: Column<String> = varchar(
        "encrypted_secret",
        BaseDbConstraints.DEFAULT_MAX_LENGTH
    )
    public val isConfirmed: Column<Boolean> = bool("is_confirmed").default(false)
    public val encryptedRecoveryCodes: Column<List<String>?> = jsonb<List<String>>(
        "encrypted_recovery_codes",
        FoundationJson,
        serializer<List<String>>()
    ).nullable()
    public val lastUsedAt: Column<Instant?> = timestamp("last_used_at").nullable()
}
