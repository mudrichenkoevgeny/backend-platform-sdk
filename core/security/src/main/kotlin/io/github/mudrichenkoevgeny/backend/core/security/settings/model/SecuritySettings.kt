package io.github.mudrichenkoevgeny.backend.core.security.settings.model

import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.model.PasswordPolicy

data class SecuritySettings(
    val passwordPolicy: PasswordPolicy
)