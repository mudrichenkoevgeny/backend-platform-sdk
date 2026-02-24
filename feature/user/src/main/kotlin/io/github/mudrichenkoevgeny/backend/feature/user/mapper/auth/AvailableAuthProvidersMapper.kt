package io.github.mudrichenkoevgeny.backend.feature.user.mapper.auth

import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AvailableAuthProviders
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.response.auth.settings.AvailableAuthProvidersResponse

fun AvailableAuthProviders.toAvailableAuthProvidersResponse() = AvailableAuthProvidersResponse(
    primary = primary.map { it.serialName },
    secondary = secondary.map { it.serialName }
)