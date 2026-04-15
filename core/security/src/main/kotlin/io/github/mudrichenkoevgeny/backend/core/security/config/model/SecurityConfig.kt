package io.github.mudrichenkoevgeny.backend.core.security.config.model

import io.github.mudrichenkoevgeny.backend.core.security.config.factory.SecurityConfigFactory
import io.github.mudrichenkoevgeny.backend.core.security.authenticationpolicychecker.AuthenticationPolicyChecker
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicy

/**
 * Runtime security configuration resolved at application startup.
 *
 * This config is produced by [SecurityConfigFactory] (typically backed by environment variables) and
 * injected where security-related decisions are made (e.g. authentication confirmation freshness,
 * default password policy).
 *
 * [AuthenticationPolicyChecker] applies these windows: self-service via
 * [AuthenticationPolicyChecker.isAuthenticationConfirmedRecently], management via
 * [AuthenticationPolicyChecker.isAuthenticationConfirmedRecentlyForManagement].
 *
 * @property recentAuthenticationValidityInMinutes How long (in minutes) a recent re-auth remains acceptable
 * for sensitive **self-service** actions (see [AuthenticationPolicyChecker.isAuthenticationConfirmedRecently]).
 * @property recentAuthenticationValidityInMinutesForManagement How long (in minutes) a recent re-auth remains
 * acceptable for sensitive **management** actions
 * (see [AuthenticationPolicyChecker.isAuthenticationConfirmedRecentlyForManagement]; typically a longer
 * window than self-service).
 * @property passwordPolicy Default password policy used as a fallback when no system setting
 * overrides it.
 */
data class SecurityConfig(
    val recentAuthenticationValidityInMinutes: Long,
    val recentAuthenticationValidityInMinutesForManagement: Long,
    val passwordPolicy: PasswordPolicy
)