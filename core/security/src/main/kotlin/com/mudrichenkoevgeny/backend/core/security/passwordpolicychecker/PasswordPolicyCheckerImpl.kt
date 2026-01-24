package com.mudrichenkoevgeny.backend.core.security.passwordpolicychecker

import com.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.enums.PasswordPolicyFailReason
import com.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.model.PasswordPolicy
import com.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.result.PasswordPolicyCheckResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasswordPolicyCheckerImpl @Inject constructor(
    private val passwordPolicy: PasswordPolicy = PasswordPolicy()
): PasswordPolicyChecker {
    override fun check(password: String): PasswordPolicyCheckResult {
        val reasons = mutableListOf<PasswordPolicyFailReason>()

        with(passwordPolicy) {
            if (password.length < minLength) {
                reasons.add(PasswordPolicyFailReason.TOO_SHORT)
            }
            if (requireLetter && !password.any { it.isLetter() }) {
                reasons.add(PasswordPolicyFailReason.NO_LETTER)
            }
            if (requireUpperCase && !password.any { it.isUpperCase() }) {
                reasons.add(PasswordPolicyFailReason.NO_UPPERCASE)
            }
            if (requireLowerCase && !password.any { it.isLowerCase() }) {
                reasons.add(PasswordPolicyFailReason.NO_LOWERCASE)
            }
            if (requireDigit && !password.any { it.isDigit() }) {
                reasons.add(PasswordPolicyFailReason.NO_DIGIT)
            }
            if (requireSpecialChar && !password.any { !it.isLetterOrDigit() }) {
                reasons.add(PasswordPolicyFailReason.NO_SPECIAL_CHAR)
            }
            if (commonPasswords.contains(password)) {
                reasons.add(PasswordPolicyFailReason.TOO_COMMON)
            }
        }

        return if (reasons.isEmpty()) {
            PasswordPolicyCheckResult.Success
        } else {
            PasswordPolicyCheckResult.Fail(reasons, passwordPolicy)
        }
    }
}