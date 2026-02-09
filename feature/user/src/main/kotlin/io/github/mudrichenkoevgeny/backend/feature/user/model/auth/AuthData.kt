package io.github.mudrichenkoevgeny.backend.feature.user.model.auth

import io.github.mudrichenkoevgeny.backend.feature.user.model.user.User

data class AuthData(
    val currentUser: User,
    val sessionToken: SessionToken
)