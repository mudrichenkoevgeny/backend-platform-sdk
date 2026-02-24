package io.github.mudrichenkoevgeny.backend.feature.user.model.configuration

import io.github.mudrichenkoevgeny.backend.core.security.settings.model.SecuritySettings
import io.github.mudrichenkoevgeny.backend.core.settings.global.model.GlobalSettings
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthSettings

data class UserConfiguration(
    val globalSettings: GlobalSettings,
    val securitySettings: SecuritySettings,
    val authSettings: AuthSettings
)