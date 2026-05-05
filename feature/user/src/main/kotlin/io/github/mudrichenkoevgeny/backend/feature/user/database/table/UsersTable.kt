package io.github.mudrichenkoevgeny.backend.feature.user.database.table

import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import kotlinx.serialization.serializer
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.json.jsonb

/**
 * Users table.
 *
 * Schema is created by a Flyway migration in `db/migration/feature/user/`.
 * The app must include this path in its Flyway migration locations.
 */
object UsersTable : BaseTable("users") {
    val role = enumerationByName("role", BaseDbConstraints.ENUM_MAX_LENGTH, UserRole::class)
    val accountStatus = enumerationByName("account_status", BaseDbConstraints.ENUM_MAX_LENGTH, UserAccountStatus::class)
    val accountStatusBeforeDeletion = enumerationByName(
        "account_status_before_deletion",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        UserAccountStatus::class
    ).nullable()
    val authorityLevel = integer("authority_level")
    val permissionCodes = jsonb<Set<String>>(
        "permission_codes",
        FoundationJson,
        serializer<Set<String>>()
    )
    val isTotpEnabled = bool("is_totp_enabled").default(false)
    val lastLoginAt = timestamp("last_login_at").nullable()
    val lastActiveAt = timestamp("last_active_at").nullable()
    val scheduledPermanentDeletionAt = timestamp("scheduled_permanent_deletion_at").nullable()
}