package io.github.mudrichenkoevgeny.backend.feature.user.model.auth

import io.github.mudrichenkoevgeny.backend.feature.user.model.user.User

/**
 * Auth result returned after a successful authentication flow.
 *
 * Combines the authenticated [User] and the issued [SessionToken].
 */
data class AuthData(
    val currentUser: User,
    val sessionToken: SessionToken
)