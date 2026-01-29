package io.github.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.model

data class PasswordPolicy(
    val minLength: Int = DEFAULT_MIN_LENGTH,
    val requireLetter: Boolean = true,
    val requireUpperCase: Boolean = false,
    val requireLowerCase: Boolean = false,
    val requireDigit: Boolean = false,
    val requireSpecialChar: Boolean = false,
    val commonPasswords: Set<String> = DEFAULT_COMMON_PASSWORDS
) {
    companion object {
        const val DEFAULT_MIN_LENGTH = 8

        val DEFAULT_COMMON_PASSWORDS = setOf(
            "12345678",
            "password",
            "123456789",
            "qwertyui",
            "abc12345",
            "11111111",
            "12312312",
            "password1",
            "iloveyou",
            "letmein1"
        )
    }
}