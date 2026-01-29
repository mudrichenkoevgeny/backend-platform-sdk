package io.github.mudrichenkoevgeny.backend.core.security.passwordpolicychecker

import io.github.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.result.PasswordPolicyCheckResult

interface PasswordPolicyChecker {
    fun check(password: String): PasswordPolicyCheckResult
}