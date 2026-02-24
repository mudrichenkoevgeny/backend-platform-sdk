package io.github.mudrichenkoevgeny.backend.core.security.settings.mapper

import io.github.mudrichenkoevgeny.backend.core.security.settings.model.SecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.response.settings.SecuritySettingsResponse
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.mapper.toPasswordPolicyResponse

fun SecuritySettings.toSecuritySettingsResponse() = SecuritySettingsResponse(
    passwordPolicy = passwordPolicy.toPasswordPolicyResponse()
)