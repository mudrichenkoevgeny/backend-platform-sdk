package io.github.mudrichenkoevgeny.backend.feature.user.database.table

import io.github.mudrichenkoevgeny.backend.feature.user.database.UserDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import org.jetbrains.exposed.v1.core.ReferenceOption
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
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