package com.mudrichenkoevgeny.backend.feature.user.model.user

import com.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier

data class UserData(
    val user: User,
    val userIdentifiersList: List<UserIdentifier>
)