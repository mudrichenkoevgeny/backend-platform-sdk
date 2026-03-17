package io.github.mudrichenkoevgeny.backend.feature.user.model.auth

/**
 * Runtime authentication settings exposed to clients.
 *
 * @property availableAuthProviders Enabled auth providers for this environment.
 */
data class AuthSettings(
    val availableAuthProviders: AvailableAuthProviders
)