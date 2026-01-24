package com.mudrichenkoevgeny.backend.feature.user.model.auth

import com.mudrichenkoevgeny.backend.feature.user.model.user.User
import com.mudrichenkoevgeny.backend.feature.user.model.useridentifier.UserIdentifier

data class AuthData(
    val user: User,
    val userIdentifiersList: List<UserIdentifier>,
    val sessionToken: SessionToken
)