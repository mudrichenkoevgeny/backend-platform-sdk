package io.github.mudrichenkoevgeny.backend.feature.user.database.table

import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.UserDbConstraints
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import org.jetbrains.exposed.v1.core.ReferenceOption

/**
 * User identifiers table (login methods linked to a user).
 *
 * Stores provider-specific identifiers (email, phone, external provider subject) and optional
 * password hashes for password-based providers.
 *
 * Schema is created by a Flyway migration in `db/migration/feature/user/`.
 * The app must include this path in its Flyway migration locations.
 */
object UserIdentifiersTable : BaseTable("user_identifiers") {
    val userId = reference("user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val userAuthProvider = enumerationByName(
        "user_auth_provider",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        UserAuthProvider::class
    )
    val identifier = varchar("identifier", BaseDbConstraints.DEFAULT_MAX_LENGTH)
    val passwordHash = varchar("password_hash", UserDbConstraints.PASSWORD_HASH_MAX_LENGTH).nullable()
}