package io.github.mudrichenkoevgeny.backend.feature.user.auth.model

import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider

data class ExternalAuthProviderData(
    val authProvider: UserAuthProvider,
    val externalId: String
)