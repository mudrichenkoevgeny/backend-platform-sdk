package io.github.mudrichenkoevgeny.backend.core.security.authenticationpolicychecker

import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicy
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class AuthenticationPolicyCheckerImplTest {

    @Test
    fun `isAuthenticationConfirmedRecently returns true when within configured validity window`() {
        val checker = AuthenticationPolicyCheckerImpl(
            securityConfig = SecurityConfig(
                recentAuthenticationValidityInMinutes = 60,
                recentAuthenticationValidityInMinutesForManagement = 60,
                passwordPolicy = PasswordPolicy()
            )
        )

        val lastReauthenticatedAt = Clock.System.now() - 5.seconds

        assertTrue(checker.isAuthenticationConfirmedRecently(lastReauthenticatedAt))
    }

    @Test
    fun `isAuthenticationConfirmedRecently returns false when outside configured validity window`() {
        val checker = AuthenticationPolicyCheckerImpl(
            securityConfig = SecurityConfig(
                recentAuthenticationValidityInMinutes = 1,
                recentAuthenticationValidityInMinutesForManagement = 60,
                passwordPolicy = PasswordPolicy()
            )
        )

        val lastReauthenticatedAt = Clock.System.now() - 1.hours

        assertFalse(checker.isAuthenticationConfirmedRecently(lastReauthenticatedAt))
    }

    @Test
    fun `isAuthenticationConfirmedRecentlyForManagement uses management validity window`() {
        val checker = AuthenticationPolicyCheckerImpl(
            securityConfig = SecurityConfig(
                recentAuthenticationValidityInMinutes = 1,
                recentAuthenticationValidityInMinutesForManagement = 60,
                passwordPolicy = PasswordPolicy()
            )
        )

        val thirtyMinutesAgo = Clock.System.now() - 30.minutes

        assertFalse(checker.isAuthenticationConfirmedRecently(thirtyMinutesAgo))
        assertTrue(checker.isAuthenticationConfirmedRecentlyForManagement(thirtyMinutesAgo))
    }

    @Test
    fun `isAuthenticationConfirmedRecentlyForManagement returns false when outside management window`() {
        val checker = AuthenticationPolicyCheckerImpl(
            securityConfig = SecurityConfig(
                recentAuthenticationValidityInMinutes = 60,
                recentAuthenticationValidityInMinutesForManagement = 1,
                passwordPolicy = PasswordPolicy()
            )
        )

        val lastReauthenticatedAt = Clock.System.now() - 1.hours

        assertFalse(checker.isAuthenticationConfirmedRecentlyForManagement(lastReauthenticatedAt))
    }

    @Test
    fun `both checks return false when lastReauthenticatedAt is null`() {
        val checker = AuthenticationPolicyCheckerImpl(
            securityConfig = SecurityConfig(
                recentAuthenticationValidityInMinutes = 60,
                recentAuthenticationValidityInMinutesForManagement = 60,
                passwordPolicy = PasswordPolicy()
            )
        )

        assertFalse(checker.isAuthenticationConfirmedRecently(null))
        assertFalse(checker.isAuthenticationConfirmedRecentlyForManagement(null))
    }
}
