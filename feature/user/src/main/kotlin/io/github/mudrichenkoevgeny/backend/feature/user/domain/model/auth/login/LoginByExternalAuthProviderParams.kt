package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.auth.login

import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider

/**
 * Domain-layer parameter holder for external provider authentication.
 *
 * @property authProvider target external identity provider (e.g., Google, Apple)
 * @property token identity token provided by the external OAuth2/OIDC provider
 * @property displayName optional profile display name override
 */
data class LoginByExternalAuthProviderParams(
    val authProvider: UserAuthProvider,
    val token: String,
    val displayName: String? = null
)
