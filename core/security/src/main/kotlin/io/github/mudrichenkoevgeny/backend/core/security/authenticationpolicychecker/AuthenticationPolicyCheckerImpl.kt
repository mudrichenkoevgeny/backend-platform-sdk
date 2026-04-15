package io.github.mudrichenkoevgeny.backend.core.security.authenticationpolicychecker

import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Default [AuthenticationPolicyChecker] implementation based on [SecurityConfig].
 *
 * Self-service: `lastReauthenticatedAt + recentAuthenticationValidityInMinutes` after `Clock.System.now()`.
 * Management: same with `recentAuthenticationValidityInMinutesForManagement`.
 */
@Singleton
class AuthenticationPolicyCheckerImpl @Inject constructor(
    private val securityConfig: SecurityConfig
) : AuthenticationPolicyChecker {
    override fun isAuthenticationConfirmedRecently(lastReauthenticatedAt: Instant?): Boolean {
        return isWithinValidityWindow(
            lastReauthenticatedAt = lastReauthenticatedAt,
            validityMinutes = securityConfig.recentAuthenticationValidityInMinutes
        )
    }

    override fun isAuthenticationConfirmedRecentlyForManagement(lastReauthenticatedAt: Instant?): Boolean {
        return isWithinValidityWindow(
            lastReauthenticatedAt = lastReauthenticatedAt,
            validityMinutes = securityConfig.recentAuthenticationValidityInMinutesForManagement
        )
    }

    private fun isWithinValidityWindow(lastReauthenticatedAt: Instant?, validityMinutes: Long): Boolean {
        val at = lastReauthenticatedAt ?: return false
        val validityEnd = at + validityMinutes.minutes
        return validityEnd > Clock.System.now()
    }
}
