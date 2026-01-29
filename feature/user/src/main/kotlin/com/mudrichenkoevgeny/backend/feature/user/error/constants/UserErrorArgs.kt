package io.github.mudrichenkoevgeny.backend.feature.user.error.constants

object UserErrorArgs {
    const val USER_ID = "userId"

    const val PASSWORD_MIN_LENGTH = "passwordMinLength"
    const val PASSWORD_FAIL_TOO_SHORT = "passwordTooShort"
    const val PASSWORD_FAIL_NO_LETTER = "passwordNoLetter"
    const val PASSWORD_FAIL_NO_UPPERCASE = "passwordNoUpperCase"
    const val PASSWORD_FAIL_NO_LOWERCASE = "passwordNoLowerCase"
    const val PASSWORD_FAIL_NO_DIGIT = "passwordNoDigit"
    const val PASSWORD_FAIL_NO_SPECIAL_CHAR = "passwordNoSpecialChar"
    const val PASSWORD_FAIL_TOO_COMMON = "passwordTooCommon"
}