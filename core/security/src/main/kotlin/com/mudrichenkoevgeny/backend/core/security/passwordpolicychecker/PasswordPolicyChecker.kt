package com.mudrichenkoevgeny.backend.core.security.passwordpolicychecker

import com.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.result.PasswordPolicyCheckResult

interface PasswordPolicyChecker {
    fun check(password: String): PasswordPolicyCheckResult
}