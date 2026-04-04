package io.github.mudrichenkoevgeny.backend.core.security.config.model

import io.github.mudrichenkoevgeny.backend.core.security.config.factory.SecurityConfigFactory
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicy

/**
 * Runtime security configuration resolved at application startup.
 *
 * This config is produced by [SecurityConfigFactory] (typically backed by environment variables) and
 * injected where security-related decisions are made (e.g. authentication confirmation freshness,
 * default password policy).
 *
 * @property recentAuthenticationValidityInMinutes How long (in minutes) a recent re-auth remains acceptable for sensitive actions.
 * @property passwordPolicy Default password policy used as a fallback when no system setting
 * overrides it.
 */
data class SecurityConfig(
    val recentAuthenticationValidityInMinutes : Long,
    val passwordPolicy: PasswordPolicy
)