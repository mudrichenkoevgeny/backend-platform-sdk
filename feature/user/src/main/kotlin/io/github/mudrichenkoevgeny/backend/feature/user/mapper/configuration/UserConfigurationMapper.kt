package io.github.mudrichenkoevgeny.backend.feature.user.mapper.configuration

import io.github.mudrichenkoevgeny.backend.core.security.settings.mapper.toSecuritySettingsResponse
import io.github.mudrichenkoevgeny.backend.core.settings.global.mapper.toGlobalSettingsResponse
import io.github.mudrichenkoevgeny.backend.feature.user.mapper.auth.toAuthSettingsResponse
import io.github.mudrichenkoevgeny.backend.feature.user.model.configuration.UserConfiguration
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.response.configuration.UserConfigurationResponse

fun UserConfiguration.toUserConfigurationResponse() = UserConfigurationResponse(
    globalSettings = globalSettings.toGlobalSettingsResponse(),
    securitySettings = securitySettings.toSecuritySettingsResponse(),
    authSettings = authSettings.toAuthSettingsResponse()
)