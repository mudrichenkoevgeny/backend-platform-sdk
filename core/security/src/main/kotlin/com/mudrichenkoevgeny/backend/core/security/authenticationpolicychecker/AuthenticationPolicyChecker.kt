package com.mudrichenkoevgeny.backend.core.security.authenticationpolicychecker

import java.time.Instant

interface AuthenticationPolicyChecker {
    fun isAuthenticationConfirmedRecently(lastReauthenticatedAt: Instant): Boolean
}