package io.github.mudrichenkoevgeny.backend.core.security.domain.model.passwordpolicy

import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.ManagementPasswordPolicy

fun createTestManagementPasswordPolicy(
    minLength: Int = ManagementPasswordPolicy.DEFAULT_MIN_LENGTH,
    requireLetter: Boolean = false,
    requireUpperCase: Boolean = true,
    requireLowerCase: Boolean = true,
    requireDigit: Boolean = true,
    requireSpecialChar: Boolean = false,
    commonPasswords: Set<String> = emptySet()
) = ManagementPasswordPolicy(
    minLength = minLength,
    requireLetter = requireLetter,
    requireUpperCase = requireUpperCase,
    requireLowerCase = requireLowerCase,
    requireDigit = requireDigit,
    requireSpecialChar = requireSpecialChar,
    commonPasswords = commonPasswords
)
