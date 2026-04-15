package io.github.mudrichenkoevgeny.backend.core.security.authenticationpolicychecker

import kotlin.time.Instant

/**
 * Checks authentication policy constraints, such as how recently the user re-authenticated.
 *
 * Self-service and management flows use different configured windows on `SecurityConfig`
 * (`recentAuthenticationValidityInMinutes` vs `recentAuthenticationValidityInMinutesForManagement`).
 */
interface AuthenticationPolicyChecker {
    /**
     * Returns `true` when the user is considered "recently re-authenticated" for **self-service**
     * sensitive actions (`SecurityConfig.recentAuthenticationValidityInMinutes`).
     *
     * @param lastReauthenticatedAt Moment when the user last passed a strong confirmation step; `null` yields `false`.
     */
    fun isAuthenticationConfirmedRecently(lastReauthenticatedAt: Instant?): Boolean

    /**
     * Returns `true` when the user is considered "recently re-authenticated" for **management**
     * sensitive actions (`SecurityConfig.recentAuthenticationValidityInMinutesForManagement`).
     *
     * @param lastReauthenticatedAt Moment when the user last passed a strong confirmation step; `null` yields `false`.
     */
    fun isAuthenticationConfirmedRecentlyForManagement(lastReauthenticatedAt: Instant?): Boolean
}