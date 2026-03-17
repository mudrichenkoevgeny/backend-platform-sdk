package io.github.mudrichenkoevgeny.backend.feature.user.database.table

import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserRole
import org.jetbrains.exposed.v1.javatime.timestamp

/**
 * Users table.
 *
 * Schema is created by a Flyway migration in `db/migration/feature/user/`.
 * The app must include this path in its Flyway migration locations.
 */
object UsersTable : BaseTable("users") {
    val role = enumerationByName("role", BaseDbConstraints.ENUM_MAX_LENGTH, UserRole::class)
    val accountStatus = enumerationByName("account_status", BaseDbConstraints.ENUM_MAX_LENGTH, UserAccountStatus::class)
    val lastLoginAt = timestamp("last_login_at").nullable()
    val lastActiveAt = timestamp("last_active_at").nullable()
}