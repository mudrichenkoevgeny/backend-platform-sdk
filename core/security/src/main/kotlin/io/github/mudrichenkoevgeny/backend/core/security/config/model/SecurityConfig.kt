package io.github.mudrichenkoevgeny.backend.core.security.config.model

import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.model.PasswordPolicy

data class SecurityConfig(
    val authenticationConfirmationValidityMinutes : Long,
    val passwordPolicy: PasswordPolicy
)