package io.github.mudrichenkoevgeny.backend.core.security.authenticationpolicychecker

import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.model.PasswordPolicy
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class AuthenticationPolicyCheckerImplTest {

    @Test
    fun `isAuthenticationConfirmedRecently returns true when within configured validity window`() {
        val checker = AuthenticationPolicyCheckerImpl(
            securityConfig = SecurityConfig(
                recentAuthenticationValidityInMinutes = 60,
                passwordPolicy = PasswordPolicy()
            )
        )

        val lastReauthenticatedAt = Instant.now().minusSeconds(5)

        assertTrue(checker.isAuthenticationConfirmedRecently(lastReauthenticatedAt))
    }

    @Test
    fun `isAuthenticationConfirmedRecently returns false when outside configured validity window`() {
        val checker = AuthenticationPolicyCheckerImpl(
            securityConfig = SecurityConfig(
                recentAuthenticationValidityInMinutes = 1,
                passwordPolicy = PasswordPolicy()
            )
        )

        val lastReauthenticatedAt = Instant.now().minusSeconds(60 * 60)

        assertFalse(checker.isAuthenticationConfirmedRecently(lastReauthenticatedAt))
    }
}

