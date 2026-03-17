package io.github.mudrichenkoevgeny.backend.feature.user.mapper.auth

import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.response.auth.settings.AuthSettingsResponse

/**
 * Maps internal [AuthSettings] to the shared network response contract.
 */
fun AuthSettings.toAuthSettingsResponse() = AuthSettingsResponse(
    availableAuthProviders = availableAuthProviders.toAvailableAuthProvidersResponse()
)