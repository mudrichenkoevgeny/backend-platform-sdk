package io.github.mudrichenkoevgeny.backend.core.security.authenticationpolicychecker

import java.time.Instant

/**
 * Checks authentication policy constraints, such as how recently the user re-authenticated.
 */
interface AuthenticationPolicyChecker {
    /**
     * Returns `true` when the user is considered "recently re-authenticated" according to the
     * current policy.
     *
     * @param lastReauthenticatedAt Moment when the user last passed a strong confirmation step.
     */
    fun isAuthenticationConfirmedRecently(lastReauthenticatedAt: Instant): Boolean
}