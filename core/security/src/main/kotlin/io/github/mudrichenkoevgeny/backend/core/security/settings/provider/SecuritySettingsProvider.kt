package io.github.mudrichenkoevgeny.backend.core.security.settings.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.settings.model.SecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.model.PasswordPolicy

interface SecuritySettingsProvider {
    suspend fun initialize(): AppResult<Unit>
    fun getSettings(): AppResult<SecuritySettings>
    fun requirePasswordPolicy(): PasswordPolicy
    suspend fun updatePasswordPolicy(policy: PasswordPolicy): AppResult<Unit>
}