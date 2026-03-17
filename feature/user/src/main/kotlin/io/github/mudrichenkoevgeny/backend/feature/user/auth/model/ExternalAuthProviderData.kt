package io.github.mudrichenkoevgeny.backend.feature.user.auth.model

import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider

/**
 * Result of successful external provider token verification.
 *
 * @param authProvider external provider that issued/validated the token
 * @param externalId stable subject identifier extracted from the provider token
 */
data class ExternalAuthProviderData(
    val authProvider: UserAuthProvider,
    val externalId: String
)