package io.github.mudrichenkoevgeny.backend.core.security.error.mapper

import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicyFailReason
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicyValidatorResult
import io.github.mudrichenkoevgeny.shared.foundation.core.security.error.naming.SecurityErrorArgs

fun PasswordPolicyValidatorResult.Fail.convertPasswordPolicyFailToAppError(): SecurityError.PasswordTooWeak {
    val errorArgs: MutableMap<String, Any> = mutableMapOf()

    errorArgs[SecurityErrorArgs.PASSWORD_MIN_LENGTH] = passwordPolicy.minLength
    errorArgs[SecurityErrorArgs.PASSWORD_FAIL_TOO_SHORT] = reasons.contains(PasswordPolicyFailReason.TOO_SHORT)
    errorArgs[SecurityErrorArgs.PASSWORD_FAIL_NO_LETTER] = reasons.contains(PasswordPolicyFailReason.NO_LETTER)
    errorArgs[SecurityErrorArgs.PASSWORD_FAIL_NO_UPPERCASE] = reasons.contains(PasswordPolicyFailReason.NO_UPPERCASE)
    errorArgs[SecurityErrorArgs.PASSWORD_FAIL_NO_LOWERCASE] = reasons.contains(PasswordPolicyFailReason.NO_LOWERCASE)
    errorArgs[SecurityErrorArgs.PASSWORD_FAIL_NO_DIGIT] = reasons.contains(PasswordPolicyFailReason.NO_DIGIT)
    errorArgs[SecurityErrorArgs.PASSWORD_FAIL_NO_SPECIAL_CHAR] = reasons.contains(PasswordPolicyFailReason.NO_SPECIAL_CHAR)
    errorArgs[SecurityErrorArgs.PASSWORD_FAIL_TOO_COMMON] = reasons.contains(PasswordPolicyFailReason.TOO_COMMON)

    return SecurityError.PasswordTooWeak(errorArgs)
}