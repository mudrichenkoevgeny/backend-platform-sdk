package io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthSettings
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AvailableAuthProviders

interface AuthSettingsProvider {
    suspend fun initialize(): AppResult<Unit>
    fun getSettings(): AppResult<AuthSettings>
    suspend fun updateAvailableAuthProviders(availableAuthProviders: AvailableAuthProviders): AppResult<Unit>
}