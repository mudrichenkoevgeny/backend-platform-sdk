package io.github.mudrichenkoevgeny.backend.core.security.settings.model

import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.model.PasswordPolicy

/**
 * Runtime security settings.
 *
 * Unlike [SecurityConfig], these settings can be stored and changed at runtime (e.g. in the
 * database-backed settings subsystem).
 *
 * @property passwordPolicy Effective password policy used for password validation.
 */
data class SecuritySettings(
    val passwordPolicy: PasswordPolicy
)