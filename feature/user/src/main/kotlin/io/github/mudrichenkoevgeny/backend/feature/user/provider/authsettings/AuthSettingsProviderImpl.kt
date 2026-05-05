package io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.flatMapSuccess
import io.github.mudrichenkoevgeny.backend.core.common.result.mapSuccess
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
 */
@Singleton
class AuthSettingsProviderImpl @Inject constructor(
    private val settingsService: SystemSettingsService,
    private val config: UserConfig
) : AuthSettingsProvider {

    override suspend fun initialize(): AppResult<Unit> {
        val defaults = config.managementAuthSettings
        return settingsService.registerDefault(
            key = KEY_AVAILABLE_AUTH_PROVIDERS,
            value = FoundationJson.encodeToString(defaults.availableAuthProviders.toAvailableAuthProvidersPayload()),
            type = SettingType.JSON
        ).flatMapSuccess {
            settingsService.registerDefault(
                key = KEY_MAX_TOTAL_IDENTIFIERS,
                value = "${defaults.maxTotalIdentifiers}",
                type = SettingType.INT
            )
        }.flatMapSuccess {
            settingsService.registerDefault(
                key = KEY_MAX_EMAIL_IDENTIFIERS,
                value = "${defaults.maxEmailIdentifiers}",
                type = SettingType.INT
            )
        }.flatMapSuccess {
            settingsService.registerDefault(
                key = KEY_MAX_PHONE_IDENTIFIERS,
                value = "${defaults.maxPhoneIdentifiers}",
                type = SettingType.INT
            )
        }.flatMapSuccess {
            settingsService.registerDefault(
                key = KEY_MAX_IDENTIFIERS_PER_EXTERNAL_PROVIDER,
                value = "${defaults.maxIdentifiersPerExternalProvider}",
                type = SettingType.INT
            )
        }.flatMapSuccess {
            settingsService.registerDefault(
                key = KEY_MAX_ACTIVE_SESSIONS,
                value = "${defaults.maxActiveSessions}",
                type = SettingType.INT
            )
        }.flatMapSuccess {
            settingsService.registerDefault(
                key = KEY_ACCESS_TOKEN_EXPIRATION_SECONDS,
                value = "${defaults.accessTokenExpirationSeconds}",
                type = SettingType.INT
            )
        }.flatMapSuccess {
            settingsService.registerDefault(
                key = KEY_REFRESH_TOKEN_EXPIRATION_SECONDS,
                value = "${defaults.refreshTokenExpirationSeconds}",
                type = SettingType.INT
            )
        }.flatMapSuccess {
            settingsService.registerDefault(
                key = KEY_ACCOUNT_DELETION_DELAY_SECONDS,
                value = "${defaults.accountDeletionDelaySeconds}",
                type = SettingType.INT
            )
        }
    }

    override fun getManagementAuthSettings(): ManagementAuthSettings {
        return ManagementAuthSettings(
            availableAuthProviders = getAvailableAuthProviders(),
            maxTotalIdentifiers = getMaxTotalIdentifiers(),
            maxEmailIdentifiers = getMaxEmailIdentifiers(),
            maxPhoneIdentifiers = getMaxPhoneIdentifiers(),
            maxIdentifiersPerExternalProvider = getMaxIdentifiersPerExternalProvider(),
            maxActiveSessions = getMaxActiveSessions(),
            accessTokenExpirationSeconds = getAccessTokenExpirationSeconds(),
            refreshTokenExpirationSeconds = getRefreshTokenExpirationSeconds(),
            accountDeletionDelaySeconds = getAccountDeletionDelaySeconds()
        )
    }

    override fun getPublicAuthSettings(): PublicAuthSettings {
        return PublicAuthSettings(
            availableAuthProviders = getAvailableAuthProviders(),
            maxTotalIdentifiers = getMaxTotalIdentifiers(),
            maxEmailIdentifiers = getMaxEmailIdentifiers(),
            maxPhoneIdentifiers = getMaxPhoneIdentifiers(),
            maxIdentifiersPerExternalProvider = getMaxIdentifiersPerExternalProvider()
        )
    }

    override fun getAvailableAuthProviders(): AvailableAuthProviders {
        return settingsService.getJson(KEY_AVAILABLE_AUTH_PROVIDERS) { json ->
            FoundationJson.decodeFromString<AvailableAuthProviders>(json)
        } ?: config.managementAuthSettings.availableAuthProviders
    }

    override fun getMaxTotalIdentifiers(): Int {
        return settingsService.getInt(KEY_MAX_TOTAL_IDENTIFIERS)
            ?: config.managementAuthSettings.maxTotalIdentifiers
    }

    override fun getMaxEmailIdentifiers(): Int {
        return settingsService.getInt(KEY_MAX_EMAIL_IDENTIFIERS)
            ?: config.managementAuthSettings.maxEmailIdentifiers
    }

    override fun getMaxPhoneIdentifiers(): Int {
        return settingsService.getInt(KEY_MAX_PHONE_IDENTIFIERS)
            ?: config.managementAuthSettings.maxPhoneIdentifiers
    }

    override fun getMaxIdentifiersPerExternalProvider(): Int {
        return settingsService.getInt(KEY_MAX_IDENTIFIERS_PER_EXTERNAL_PROVIDER)
            ?: config.managementAuthSettings.maxIdentifiersPerExternalProvider
    }

    override fun getMaxActiveSessions(): Int {
        return settingsService.getInt(KEY_MAX_ACTIVE_SESSIONS)
            ?: config.managementAuthSettings.maxActiveSessions
    }

    override fun getAccessTokenExpirationSeconds(): Int {
        return settingsService.getInt(KEY_ACCESS_TOKEN_EXPIRATION_SECONDS)
            ?: config.managementAuthSettings.accessTokenExpirationSeconds
    }

    override fun getRefreshTokenExpirationSeconds(): Int {
        return settingsService.getInt(KEY_REFRESH_TOKEN_EXPIRATION_SECONDS)
            ?: config.managementAuthSettings.refreshTokenExpirationSeconds
    }

    override fun getAccountDeletionDelaySeconds(): Int {
        return settingsService.getInt(KEY_ACCOUNT_DELETION_DELAY_SECONDS)
            ?: config.managementAuthSettings.accountDeletionDelaySeconds
    }

    override suspend fun updateManagementAuthSettings(
        managementAuthSettings: ManagementAuthSettings
    ): AppResult<Unit> {
        return settingsService.updateSetting(
            key = KEY_AVAILABLE_AUTH_PROVIDERS,
            value = FoundationJson.encodeToString(managementAuthSettings.availableAuthProviders.toAvailableAuthProvidersPayload()),
            type = SettingType.JSON
        ).flatMapSuccess {
            settingsService.updateSetting(
                key = KEY_MAX_TOTAL_IDENTIFIERS,
                value = "${managementAuthSettings.maxTotalIdentifiers}",
                type = SettingType.INT
            )
        }.flatMapSuccess {
            settingsService.updateSetting(
                key = KEY_MAX_EMAIL_IDENTIFIERS,
                value = "${managementAuthSettings.maxEmailIdentifiers}",
                type = SettingType.INT
            )
        }.flatMapSuccess {
            settingsService.updateSetting(
                key = KEY_MAX_PHONE_IDENTIFIERS,
                value = "${managementAuthSettings.maxPhoneIdentifiers}",
                type = SettingType.INT
            )
        }.flatMapSuccess {
            settingsService.updateSetting(
                key = KEY_MAX_IDENTIFIERS_PER_EXTERNAL_PROVIDER,
                value = "${managementAuthSettings.maxIdentifiersPerExternalProvider}",
                type = SettingType.INT
            )
        }.flatMapSuccess {
            settingsService.updateSetting(
                key = KEY_MAX_ACTIVE_SESSIONS,
                value = "${managementAuthSettings.maxActiveSessions}",
                type = SettingType.INT
            )
        }.flatMapSuccess {
            settingsService.updateSetting(
                key = KEY_ACCESS_TOKEN_EXPIRATION_SECONDS,
                value = "${managementAuthSettings.accessTokenExpirationSeconds}",
                type = SettingType.INT
            )
        }.flatMapSuccess {
            settingsService.updateSetting(
                key = KEY_REFRESH_TOKEN_EXPIRATION_SECONDS,
                value = "${managementAuthSettings.refreshTokenExpirationSeconds}",
                type = SettingType.INT
            )
        }.flatMapSuccess {
            settingsService.updateSetting(
                key = KEY_ACCOUNT_DELETION_DELAY_SECONDS,
                value = "${managementAuthSettings.accountDeletionDelaySeconds}",
                type = SettingType.INT
            )
        }.mapSuccess { }
    }

    private companion object {
        const val KEY_AVAILABLE_AUTH_PROVIDERS = "auth.available_auth_providers"
        const val KEY_MAX_TOTAL_IDENTIFIERS = "auth.max_total_identifiers"
        const val KEY_MAX_EMAIL_IDENTIFIERS = "auth.max_email_identifiers"
        const val KEY_MAX_PHONE_IDENTIFIERS = "auth.max_phone_identifiers"
        const val KEY_MAX_IDENTIFIERS_PER_EXTERNAL_PROVIDER = "auth.max_identifiers_per_external_provider"
        const val KEY_MAX_ACTIVE_SESSIONS = "auth.max_active_sessions"
        const val KEY_ACCESS_TOKEN_EXPIRATION_SECONDS = "auth.access_token_expiration_seconds"
        const val KEY_REFRESH_TOKEN_EXPIRATION_SECONDS = "auth.refresh_token_expiration_seconds"
        const val KEY_ACCOUNT_DELETION_DELAY_SECONDS = "auth.account_deletion_delay_seconds"
    }
}
