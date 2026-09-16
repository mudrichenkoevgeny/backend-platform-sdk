package io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapSuccess
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.AvailableAuthProviders
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.OpenAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.emailrestriction.EmailRestrictionPolicy
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.auth.settings.toAvailableAuthProvidersPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.emailrestriction.toEmailRestrictionPolicy
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.mapper.emailrestriction.toEmailRestrictionPolicyPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.model.emailrestriction.EmailRestrictionPolicyPayload
import java.util.concurrent.atomic.AtomicReference
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

    private val openEmailRestrictionPolicyCache = AtomicReference<Pair<String, EmailRestrictionPolicy>?>()
    private val managementEmailRestrictionPolicyCache = AtomicReference<Pair<String, EmailRestrictionPolicy>?>()

    override suspend fun initialize(): AppResult<Unit> {
        val defaults = config.managementAuthSettings
        val defaultSettings = listOf(
            SystemSetting(
                key = KEY_AVAILABLE_AUTH_PROVIDERS,
                value = FoundationJson.encodeToString(defaults.availableAuthProviders.toAvailableAuthProvidersPayload()),
                type = SettingType.JSON
            ),
            SystemSetting(
                key = KEY_MAX_TOTAL_IDENTIFIERS,
                value = "${defaults.maxTotalIdentifiers}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_MAX_EMAIL_IDENTIFIERS,
                value = "${defaults.maxEmailIdentifiers}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_MAX_PHONE_IDENTIFIERS,
                value = "${defaults.maxPhoneIdentifiers}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_MAX_IDENTIFIERS_PER_EXTERNAL_PROVIDER,
                value = "${defaults.maxIdentifiersPerExternalProvider}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_MAX_ACTIVE_SESSIONS_FOR_OPEN_USER,
                value = "${defaults.maxActiveSessionsForOpenUser}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_MAX_ACTIVE_SESSIONS_FOR_MANAGEMENT_USER,
                value = "${defaults.maxActiveSessionsForManagementUser}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_ACCESS_TOKEN_EXPIRATION_SECONDS,
                value = "${defaults.accessTokenExpirationSeconds}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_REFRESH_TOKEN_EXPIRATION_SECONDS,
                value = "${defaults.refreshTokenExpirationSeconds}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_ACCOUNT_DELETION_GRACE_PERIOD_SECONDS,
                value = "${defaults.accountDeletionGracePeriodSeconds}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_ACCOUNT_DELETION_CHECK_INTERVAL_SECONDS,
                value = "${defaults.accountDeletionCheckIntervalSeconds}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_IS_REGISTRATION_ENABLED,
                value = "${defaults.isRegistrationEnabled}",
                type = SettingType.BOOLEAN
            ),
            SystemSetting(
                key = KEY_OPEN_EMAIL_RESTRICTION_POLICY,
                value = FoundationJson.encodeToString(defaults.openEmailRestrictionPolicy.toEmailRestrictionPolicyPayload()),
                type = SettingType.JSON
            ),
            SystemSetting(
                key = KEY_MANAGEMENT_EMAIL_RESTRICTION_POLICY,
                value = FoundationJson.encodeToString(defaults.managementEmailRestrictionPolicy.toEmailRestrictionPolicyPayload()),
                type = SettingType.JSON
            )
        )
        return settingsService.registerDefaults(defaultSettings)
    }

    override fun getManagementAuthSettings(): ManagementAuthSettings {
        return ManagementAuthSettings(
            availableAuthProviders = getAvailableAuthProviders(),
            maxTotalIdentifiers = getMaxTotalIdentifiers(),
            maxEmailIdentifiers = getMaxEmailIdentifiers(),
            maxPhoneIdentifiers = getMaxPhoneIdentifiers(),
            maxIdentifiersPerExternalProvider = getMaxIdentifiersPerExternalProvider(),
            maxActiveSessionsForOpenUser = getMaxActiveSessionsForOpenUser(),
            maxActiveSessionsForManagementUser = getMaxActiveSessionsForManagementUser(),
            accessTokenExpirationSeconds = getAccessTokenExpirationSeconds(),
            refreshTokenExpirationSeconds = getRefreshTokenExpirationSeconds(),
            accountDeletionGracePeriodSeconds = getAccountDeletionGracePeriodSeconds(),
            accountDeletionCheckIntervalSeconds = getAccountDeletionCheckIntervalSeconds(),
            isRegistrationEnabled = getIsRegistrationEnabled(),
            openEmailRestrictionPolicy = getOpenEmailRestrictionPolicy(),
            managementEmailRestrictionPolicy = getManagementEmailRestrictionPolicy()
        )
    }

    override fun getOpenAuthSettings(): OpenAuthSettings {
        return OpenAuthSettings(
            availableAuthProviders = getAvailableAuthProviders(),
            maxTotalIdentifiers = getMaxTotalIdentifiers(),
            maxEmailIdentifiers = getMaxEmailIdentifiers(),
            maxPhoneIdentifiers = getMaxPhoneIdentifiers(),
            maxIdentifiersPerExternalProvider = getMaxIdentifiersPerExternalProvider(),
            isRegistrationEnabled = getIsRegistrationEnabled()
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

    override fun getMaxActiveSessionsForOpenUser(): Int {
        return settingsService.getInt(KEY_MAX_ACTIVE_SESSIONS_FOR_OPEN_USER)
            ?: config.managementAuthSettings.maxActiveSessionsForOpenUser
    }

    override fun getMaxActiveSessionsForManagementUser(): Int {
        return settingsService.getInt(KEY_MAX_ACTIVE_SESSIONS_FOR_MANAGEMENT_USER)
            ?: config.managementAuthSettings.maxActiveSessionsForManagementUser
    }

    override fun getAccessTokenExpirationSeconds(): Int {
        return settingsService.getInt(KEY_ACCESS_TOKEN_EXPIRATION_SECONDS)
            ?: config.managementAuthSettings.accessTokenExpirationSeconds
    }

    override fun getRefreshTokenExpirationSeconds(): Int {
        return settingsService.getInt(KEY_REFRESH_TOKEN_EXPIRATION_SECONDS)
            ?: config.managementAuthSettings.refreshTokenExpirationSeconds
    }

    override fun getAccountDeletionGracePeriodSeconds(): Int {
        return settingsService.getInt(KEY_ACCOUNT_DELETION_GRACE_PERIOD_SECONDS)
            ?: config.managementAuthSettings.accountDeletionGracePeriodSeconds
    }

    override fun getAccountDeletionCheckIntervalSeconds(): Int {
        return settingsService.getInt(KEY_ACCOUNT_DELETION_CHECK_INTERVAL_SECONDS)
            ?: config.managementAuthSettings.accountDeletionCheckIntervalSeconds
    }

    override fun getIsRegistrationEnabled(): Boolean {
        return settingsService.getBoolean(KEY_IS_REGISTRATION_ENABLED)
            ?: config.managementAuthSettings.isRegistrationEnabled
    }

    override fun getOpenEmailRestrictionPolicy(): EmailRestrictionPolicy {
        val rawJson = settingsService.getString(KEY_OPEN_EMAIL_RESTRICTION_POLICY)
            ?: return config.managementAuthSettings.openEmailRestrictionPolicy

        val cached = openEmailRestrictionPolicyCache.get()
        if (cached != null && (cached.first == rawJson)) {
            return cached.second
        }

        return try {
            val parsed = FoundationJson.decodeFromString<EmailRestrictionPolicyPayload>(rawJson).toEmailRestrictionPolicy()
            openEmailRestrictionPolicyCache.set(rawJson to parsed)
            parsed
        } catch (_: Exception) {
            config.managementAuthSettings.openEmailRestrictionPolicy
        }
    }

    override fun getManagementEmailRestrictionPolicy(): EmailRestrictionPolicy {
        val rawJson = settingsService.getString(KEY_MANAGEMENT_EMAIL_RESTRICTION_POLICY)
            ?: return config.managementAuthSettings.managementEmailRestrictionPolicy

        val cached = managementEmailRestrictionPolicyCache.get()
        if (cached != null && (cached.first == rawJson)) {
            return cached.second
        }

        return try {
            val parsed = FoundationJson.decodeFromString<EmailRestrictionPolicyPayload>(rawJson).toEmailRestrictionPolicy()
            managementEmailRestrictionPolicyCache.set(rawJson to parsed)
            parsed
        } catch (_: Exception) {
            config.managementAuthSettings.managementEmailRestrictionPolicy
        }
    }

    override suspend fun updateManagementAuthSettings(
        managementAuthSettings: ManagementAuthSettings
    ): AppResult<Unit> {
        val settingsToUpdate = listOf(
            SystemSetting(
                key = KEY_AVAILABLE_AUTH_PROVIDERS,
                value = FoundationJson.encodeToString(managementAuthSettings.availableAuthProviders.toAvailableAuthProvidersPayload()),
                type = SettingType.JSON
            ),
            SystemSetting(
                key = KEY_MAX_TOTAL_IDENTIFIERS,
                value = "${managementAuthSettings.maxTotalIdentifiers}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_MAX_EMAIL_IDENTIFIERS,
                value = "${managementAuthSettings.maxEmailIdentifiers}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_MAX_PHONE_IDENTIFIERS,
                value = "${managementAuthSettings.maxPhoneIdentifiers}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_MAX_IDENTIFIERS_PER_EXTERNAL_PROVIDER,
                value = "${managementAuthSettings.maxIdentifiersPerExternalProvider}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_MAX_ACTIVE_SESSIONS_FOR_OPEN_USER,
                value = "${managementAuthSettings.maxActiveSessionsForOpenUser}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_MAX_ACTIVE_SESSIONS_FOR_MANAGEMENT_USER,
                value = "${managementAuthSettings.maxActiveSessionsForManagementUser}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_ACCESS_TOKEN_EXPIRATION_SECONDS,
                value = "${managementAuthSettings.accessTokenExpirationSeconds}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_REFRESH_TOKEN_EXPIRATION_SECONDS,
                value = "${managementAuthSettings.refreshTokenExpirationSeconds}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_ACCOUNT_DELETION_GRACE_PERIOD_SECONDS,
                value = "${managementAuthSettings.accountDeletionGracePeriodSeconds}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_ACCOUNT_DELETION_CHECK_INTERVAL_SECONDS,
                value = "${managementAuthSettings.accountDeletionCheckIntervalSeconds}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_IS_REGISTRATION_ENABLED,
                value = "${managementAuthSettings.isRegistrationEnabled}",
                type = SettingType.BOOLEAN
            ),
            SystemSetting(
                key = KEY_OPEN_EMAIL_RESTRICTION_POLICY,
                value = FoundationJson.encodeToString(managementAuthSettings.openEmailRestrictionPolicy.toEmailRestrictionPolicyPayload()),
                type = SettingType.JSON
            ),
            SystemSetting(
                key = KEY_MANAGEMENT_EMAIL_RESTRICTION_POLICY,
                value = FoundationJson.encodeToString(managementAuthSettings.managementEmailRestrictionPolicy.toEmailRestrictionPolicyPayload()),
                type = SettingType.JSON
            )
        )
        return settingsService.updateSettings(settingsToUpdate).mapSuccess { }
    }

    private companion object {
        const val KEY_AVAILABLE_AUTH_PROVIDERS = "auth.available_auth_providers"
        const val KEY_MAX_TOTAL_IDENTIFIERS = "auth.max_total_identifiers"
        const val KEY_MAX_EMAIL_IDENTIFIERS = "auth.max_email_identifiers"
        const val KEY_MAX_PHONE_IDENTIFIERS = "auth.max_phone_identifiers"
        const val KEY_MAX_IDENTIFIERS_PER_EXTERNAL_PROVIDER = "auth.max_identifiers_per_external_provider"
        const val KEY_MAX_ACTIVE_SESSIONS_FOR_OPEN_USER = "auth.max_active_sessions_for_open_user"
        const val KEY_MAX_ACTIVE_SESSIONS_FOR_MANAGEMENT_USER = "auth.max_active_sessions_for_management_user"
        const val KEY_ACCESS_TOKEN_EXPIRATION_SECONDS = "auth.access_token_expiration_seconds"
        const val KEY_REFRESH_TOKEN_EXPIRATION_SECONDS = "auth.refresh_token_expiration_seconds"
        const val KEY_ACCOUNT_DELETION_GRACE_PERIOD_SECONDS = "auth.account_deletion_grace_period_seconds"
        const val KEY_ACCOUNT_DELETION_CHECK_INTERVAL_SECONDS = "auth.account_deletion_check_interval_seconds"
        const val KEY_IS_REGISTRATION_ENABLED = "auth.is_registration_enabled"
        const val KEY_OPEN_EMAIL_RESTRICTION_POLICY = "auth.open_email_restriction_policy"
        const val KEY_MANAGEMENT_EMAIL_RESTRICTION_POLICY = "auth.management_email_restriction_policy"
    }
}