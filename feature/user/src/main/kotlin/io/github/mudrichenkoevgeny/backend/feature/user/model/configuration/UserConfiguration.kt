package io.github.mudrichenkoevgeny.backend.feature.user.model.configuration

import io.github.mudrichenkoevgeny.backend.core.security.settings.model.SecuritySettings
import io.github.mudrichenkoevgeny.backend.core.settings.global.model.GlobalSettings
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthSettings

/**
 * Aggregated user-facing configuration returned by the feature.
 *
 * Combines settings coming from core modules (global/security) and from the user feature itself.
 *
 * @property globalSettings Public global settings.
 * @property securitySettings Runtime security settings (e.g. password policy).
 * @property authSettings Runtime auth settings.
 */
data class UserConfiguration(
    val globalSettings: GlobalSettings,
    val securitySettings: SecuritySettings,
    val authSettings: AuthSettings
)