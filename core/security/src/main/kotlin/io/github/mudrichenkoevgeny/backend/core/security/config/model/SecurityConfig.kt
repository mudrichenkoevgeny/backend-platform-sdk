package io.github.mudrichenkoevgeny.backend.core.security.config.model

import io.github.mudrichenkoevgeny.backend.core.security.config.factory.SecurityConfigFactory
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.model.PasswordPolicy

/**
 * Runtime security configuration resolved at application startup.
 *
 * This config is produced by [SecurityConfigFactory] (typically backed by environment variables) and
 * injected where security-related decisions are made (e.g. authentication confirmation freshness,
 * default password policy).
 *
 * @property authenticationConfirmationValidityMinutes A time window in minutes during which a recent
 * re-authentication is considered valid.
 * @property passwordPolicy Default password policy used as a fallback when no system setting
 * overrides it.
 */
data class SecurityConfig(
    val authenticationConfirmationValidityMinutes : Long,
    val passwordPolicy: PasswordPolicy
)