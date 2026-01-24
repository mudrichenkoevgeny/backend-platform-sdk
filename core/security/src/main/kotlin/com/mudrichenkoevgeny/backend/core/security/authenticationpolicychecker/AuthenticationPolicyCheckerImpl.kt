package com.mudrichenkoevgeny.backend.core.security.authenticationpolicychecker

import com.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationPolicyCheckerImpl @Inject constructor(
    private val securityConfig: SecurityConfig
): AuthenticationPolicyChecker {
    override fun isAuthenticationConfirmedRecently(lastReauthenticatedAt: Instant): Boolean {
        return lastReauthenticatedAt
            .plus(Duration.ofMinutes(securityConfig.authenticationConfirmationValidityMinutes))
            .isAfter(Instant.now())
    }
}