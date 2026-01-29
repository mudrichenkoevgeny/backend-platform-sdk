package io.github.mudrichenkoevgeny.backend.feature.user.error.helper

import io.github.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.enums.PasswordPolicyFailReason
import io.github.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.result.PasswordPolicyCheckResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.constants.UserErrorArgs
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError

fun PasswordPolicyCheckResult.Fail.convertToPasswordTooWeak(): UserError.PasswordTooWeak {
    val errorArgs: MutableMap<String, Any> = mutableMapOf()

    errorArgs[UserErrorArgs.PASSWORD_MIN_LENGTH] = this.passwordPolicy.minLength
    errorArgs[UserErrorArgs.PASSWORD_FAIL_TOO_SHORT] = this.reasons.contains(PasswordPolicyFailReason.TOO_SHORT)
    errorArgs[UserErrorArgs.PASSWORD_FAIL_NO_LETTER] = this.reasons.contains(PasswordPolicyFailReason.NO_LETTER)
    errorArgs[UserErrorArgs.PASSWORD_FAIL_NO_UPPERCASE] = this.reasons.contains(PasswordPolicyFailReason.NO_UPPERCASE)
    errorArgs[UserErrorArgs.PASSWORD_FAIL_NO_LOWERCASE] = this.reasons.contains(PasswordPolicyFailReason.NO_LOWERCASE)
    errorArgs[UserErrorArgs.PASSWORD_FAIL_NO_DIGIT] = this.reasons.contains(PasswordPolicyFailReason.NO_DIGIT)
    errorArgs[UserErrorArgs.PASSWORD_FAIL_NO_SPECIAL_CHAR] = this.reasons.contains(PasswordPolicyFailReason.NO_SPECIAL_CHAR)
    errorArgs[UserErrorArgs.PASSWORD_FAIL_TOO_COMMON] = this.reasons.contains(PasswordPolicyFailReason.TOO_COMMON)

    return UserError.PasswordTooWeak(errorArgs)
}