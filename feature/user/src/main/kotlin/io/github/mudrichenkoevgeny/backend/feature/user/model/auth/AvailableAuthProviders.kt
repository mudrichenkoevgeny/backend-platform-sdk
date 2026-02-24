package io.github.mudrichenkoevgeny.backend.feature.user.model.auth

import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider

data class AvailableAuthProviders(
    val primary: List<UserAuthProvider>,
    val secondary: List<UserAuthProvider>
)
