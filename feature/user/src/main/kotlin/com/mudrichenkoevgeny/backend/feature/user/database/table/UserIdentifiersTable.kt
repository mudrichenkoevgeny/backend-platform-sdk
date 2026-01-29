package io.github.mudrichenkoevgeny.backend.feature.user.database.table

import io.github.mudrichenkoevgeny.backend.feature.user.database.UserDbConstraints
import io.github.mudrichenkoevgeny.backend.feature.user.enums.UserAuthProvider
import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import org.jetbrains.exposed.sql.ReferenceOption

object UserIdentifiersTable : BaseTable("user_identifiers") {
    val userId = reference("user_id", UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val userAuthProvider = enumerationByName(
        "user_auth_provider",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        UserAuthProvider::class
    )
    val identifier = varchar("identifier", BaseDbConstraints.DEFAULT_MAX_LENGTH)
    val passwordHash = varchar("password_hash", UserDbConstraints.PASSWORD_HASH_MAX_LENGTH).nullable()

    init {
        uniqueIndex("uk_user_identifiers_userid_provider", userId, userAuthProvider)
        uniqueIndex("uk_user_identifiers_provider_identifier", userAuthProvider, identifier)
    }
}