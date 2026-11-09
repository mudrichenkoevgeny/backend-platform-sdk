package io.github.mudrichenkoevgeny.backend.feature.user.database.table

import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.backend.feature.user.database.UserDbConstraints
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import kotlin.uuid.Uuid

/**
 * User identifiers table (login methods linked to a user).
 *
 * Stores provider-specific identifiers (email, phone, external provider subject) and optional
 * password hashes for password-based providers.
 *
 * Schema is created by a Flyway migration in `db/migration/feature/user/`.
 * The app must include this path in its Flyway migration locations.
 */
public object UserIdentifiersTable : BaseTable("user_identifiers") {
    public val userId: Column<EntityID<Uuid>> = reference(
        "user_id",
        UsersTable.id,
        onDelete = ReferenceOption.CASCADE
    )
    public val userAuthProvider: Column<UserAuthProvider> = enumerationByName(
        "user_auth_provider",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        UserAuthProvider::class
    )
    public val identifier: Column<String> = varchar(
        "identifier",
        BaseDbConstraints.DEFAULT_MAX_LENGTH
    )
    public val displayName: Column<String> = varchar(
        "display_name",
        BaseDbConstraints.DEFAULT_MAX_LENGTH
    )
    public val externalProviderEmail: Column<String?> = varchar(
        "external_provider_email",
        BaseDbConstraints.DEFAULT_MAX_LENGTH
    ).nullable()
    public val passwordHash: Column<String?> = varchar(
        "password_hash",
        UserDbConstraints.PASSWORD_HASH_MAX_LENGTH
    ).nullable()
}
