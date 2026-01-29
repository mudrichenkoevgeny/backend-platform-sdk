package io.github.mudrichenkoevgeny.backend.feature.user.model.user

import io.github.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier

data class UserData(
    val user: User,
    val userIdentifiersList: List<UserIdentifier>
)