package io.github.mudrichenkoevgeny.backend.feature.user.mapper.auth

import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.response.auth.settings.AuthSettingsResponse

fun AuthSettings.toAuthSettingsResponse() = AuthSettingsResponse(
    availableAuthProviders = availableAuthProviders.toAvailableAuthProvidersResponse()
)