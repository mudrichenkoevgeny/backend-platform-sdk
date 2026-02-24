package io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthSettings
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AvailableAuthProviders
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthSettingsProviderImpl @Inject constructor(
    private val settingsService: SystemSettingsService,
    private val authSettings: AuthSettings
) : AuthSettingsProvider {

    override suspend fun initialize(): AppResult<Unit> {
        val availableAuthProvidersJson = FoundationJson.encodeToString(authSettings.availableAuthProviders)

        return settingsService.registerDefault(
            key = KEY_AVAILABLE_AUTH_PROVIDERS,
            value = availableAuthProvidersJson,
            type = SettingType.JSON
        )
    }

    override fun getSettings(): AppResult<AuthSettings> {
        val settings = AuthSettings(
            availableAuthProviders = settingsService.getJson(KEY_AVAILABLE_AUTH_PROVIDERS) { json ->
                FoundationJson.decodeFromString<AvailableAuthProviders>(json)
            } ?: AvailableAuthProviders(
                primary = emptyList(),
                secondary = emptyList()
            )
        )

        return AppResult.Success(settings)
    }

    override suspend fun updateAvailableAuthProviders(
        availableAuthProviders: AvailableAuthProviders
    ): AppResult<Unit> {
        val jsonValue = FoundationJson.encodeToString(availableAuthProviders)
        val result = settingsService.updateSetting(KEY_AVAILABLE_AUTH_PROVIDERS, jsonValue, SettingType.JSON)

        return when (result) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Error -> AppResult.Error(result.error)
        }
    }

    private companion object {
        const val KEY_AVAILABLE_AUTH_PROVIDERS = "auth.available_auth_providers"
    }
}