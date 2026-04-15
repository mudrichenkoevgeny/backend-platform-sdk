package io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.flatMapSuccess
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.AvailableAuthProviders
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.PublicAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.auth.settings.toAvailableAuthProvidersPayload
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AuthSettingsProvider] implementation backed by [SystemSettingsService].
 *
 * Stores available providers as JSON and token validity as long values. Missing or invalid JSON and missing longs
 * fall back to [UserConfig.managementAuthSettings].
 */
@Singleton
class AuthSettingsProviderImpl @Inject constructor(
    private val settingsService: SystemSettingsService,
    private val config: UserConfig
) : AuthSettingsProvider {

    override suspend fun initialize(): AppResult<Unit> {
        val defaults = config.managementAuthSettings
        return settingsService.registerDefault(
            key = KEY_ACCESS_TOKEN_VALIDITY_HOURS,
            value = "${defaults.accessTokenValidityHours}",
            type = SettingType.LONG
        ).flatMapSuccess {
            settingsService.registerDefault(
                key = KEY_REFRESH_TOKEN_VALIDITY_DAYS,
                value = "${defaults.refreshTokenValidityDays}",
                type = SettingType.LONG
            )
        }.flatMapSuccess {
            settingsService.registerDefault(
                key = KEY_AVAILABLE_AUTH_PROVIDERS,
                value = FoundationJson.encodeToString(
                    defaults.availableAuthProviders.toAvailableAuthProvidersPayload()
                ),
                type = SettingType.JSON
            )
        }
    }

    override fun getManagementAuthSettings(): AppResult<ManagementAuthSettings> {
        val settings = ManagementAuthSettings(
            availableAuthProviders = readAvailableAuthProviders(),
            accessTokenValidityHours = readAccessTokenValidityHours(),
            refreshTokenValidityDays = readRefreshTokenValidityDays()
        )
        return AppResult.Success(settings)
    }

    override fun getPublicAuthSettings(): AppResult<PublicAuthSettings> {
        return AppResult.Success(
            PublicAuthSettings(availableAuthProviders = readAvailableAuthProviders())
        )
    }

    override suspend fun updateManagementAuthSettings(
        managementAuthSettings: ManagementAuthSettings
    ): AppResult<Unit> {
        return settingsService.updateSetting(
            key = KEY_ACCESS_TOKEN_VALIDITY_HOURS,
            value = "${managementAuthSettings.accessTokenValidityHours}",
            type = SettingType.LONG
        ).flatMapSuccess {
            settingsService.updateSetting(
                key = KEY_REFRESH_TOKEN_VALIDITY_DAYS,
                value = "${managementAuthSettings.refreshTokenValidityDays}",
                type = SettingType.LONG
            )
        }.flatMapSuccess {
            settingsService.updateSetting(
                key = KEY_AVAILABLE_AUTH_PROVIDERS,
                value = FoundationJson.encodeToString(
                    managementAuthSettings.availableAuthProviders.toAvailableAuthProvidersPayload()
                ),
                type = SettingType.JSON
            )
        }.flatMapSuccess { AppResult.Success(Unit) }
    }

    private fun readAvailableAuthProviders(): AvailableAuthProviders {
        return settingsService.getJson(KEY_AVAILABLE_AUTH_PROVIDERS) { json ->
            FoundationJson.decodeFromString<AvailableAuthProviders>(json)
        } ?: config.managementAuthSettings.availableAuthProviders
    }

    private fun readAccessTokenValidityHours(): Long {
        return settingsService.getLong(KEY_ACCESS_TOKEN_VALIDITY_HOURS)
            ?: config.managementAuthSettings.accessTokenValidityHours
    }

    private fun readRefreshTokenValidityDays(): Long {
        return settingsService.getLong(KEY_REFRESH_TOKEN_VALIDITY_DAYS)
            ?: config.managementAuthSettings.refreshTokenValidityDays
    }

    private companion object {
        const val KEY_AVAILABLE_AUTH_PROVIDERS = "auth.available_auth_providers"
        const val KEY_ACCESS_TOKEN_VALIDITY_HOURS = "auth.access_token_validity_hours"
        const val KEY_REFRESH_TOKEN_VALIDITY_DAYS = "auth.refresh_token_validity_days"
    }
}
