package io.github.mudrichenkoevgeny.backend.core.settings.global.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapSuccess
import io.github.mudrichenkoevgeny.backend.core.settings.config.model.GlobalSettingsConfig
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.ManagementGlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.OpenGlobalSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [GlobalSettingsProvider] implementation backed by [SystemSettingsService].
 */
@Singleton
class GlobalSettingsProviderImpl @Inject constructor(
    private val settingsService: SystemSettingsService,
    private val config: GlobalSettingsConfig
) : GlobalSettingsProvider {

    override suspend fun initialize(): AppResult<Unit> {
        val defaultSettings = listOf(
            SystemSetting(
                key = KEY_PRIVACY_POLICY,
                value = config.managementGlobalSettings.privacyPolicyUrl.orEmpty(),
                type = SettingType.STRING
            ),
            SystemSetting(
                key = KEY_TERMS_OF_SERVICE,
                value = config.managementGlobalSettings.termsOfServiceUrl.orEmpty(),
                type = SettingType.STRING
            ),
            SystemSetting(
                key = KEY_SUPPORT_EMAIL,
                value = config.managementGlobalSettings.contactSupportEmail.orEmpty(),
                type = SettingType.STRING
            ),
            SystemSetting(
                key = KEY_MIN_SUPPORTED_APP_VERSIONS,
                value = FoundationJson.encodeToString(config.managementGlobalSettings.minSupportedAppVersions),
                type = SettingType.JSON
            ),
            SystemSetting(
                key = KEY_IS_TRACING_ENABLED,
                value = "${config.managementGlobalSettings.isTracingEnabled}",
                type = SettingType.BOOLEAN
            ),
            SystemSetting(
                key = KEY_IS_METRICS_ENABLED,
                value = "${config.managementGlobalSettings.isMetricsEnabled}",
                type = SettingType.BOOLEAN
            ),
            SystemSetting(
                key = KEY_IS_VERBOSE_LOGGING_ENABLED,
                value = "${config.managementGlobalSettings.isVerboseLoggingEnabled}",
                type = SettingType.BOOLEAN
            )
        )
        return settingsService.registerDefaults(defaultSettings)
    }

    override fun getOpenGlobalSettings(): OpenGlobalSettings {
        return OpenGlobalSettings(
            privacyPolicyUrl = getPrivacyPolicyUrl(),
            termsOfServiceUrl = getTermsOfServiceUrl(),
            contactSupportEmail = getContactSupportEmail(),
            maintenanceUntilEpochMillis = null,
            minSupportedAppVersions = getMinSupportedAppVersions()
        )
    }

    override fun getManagementGlobalSettings(): ManagementGlobalSettings {
        return ManagementGlobalSettings(
            privacyPolicyUrl = getPrivacyPolicyUrl(),
            termsOfServiceUrl = getTermsOfServiceUrl(),
            contactSupportEmail = getContactSupportEmail(),
            maintenanceUntilEpochMillis = null,
            minSupportedAppVersions = getMinSupportedAppVersions(),
            isTracingEnabled = getIsTracingEnabled(),
            isMetricsEnabled = getIsMetricsEnabled(),
            isVerboseLoggingEnabled = getIsVerboseLoggingEnabled()
        )
    }

    override fun getPrivacyPolicyUrl(): String? {
        return settingsService.getString(KEY_PRIVACY_POLICY) ?: config.managementGlobalSettings.privacyPolicyUrl
    }

    override fun getTermsOfServiceUrl(): String? {
        return settingsService.getString(KEY_TERMS_OF_SERVICE) ?: config.managementGlobalSettings.termsOfServiceUrl
    }

    override fun getContactSupportEmail(): String? {
        return settingsService.getString(KEY_SUPPORT_EMAIL) ?: config.managementGlobalSettings.contactSupportEmail
    }

    override fun getMinSupportedAppVersions(): Map<ClientType, String> {
        return settingsService.getJson(KEY_MIN_SUPPORTED_APP_VERSIONS) { json ->
            FoundationJson.decodeFromString<Map<ClientType, String>>(json)
        } ?: config.managementGlobalSettings.minSupportedAppVersions
    }

    override fun getIsTracingEnabled(): Boolean {
        return settingsService.getBoolean(KEY_IS_TRACING_ENABLED) ?: config.managementGlobalSettings.isTracingEnabled
    }

    override fun getIsMetricsEnabled(): Boolean {
        return settingsService.getBoolean(KEY_IS_METRICS_ENABLED) ?: config.managementGlobalSettings.isMetricsEnabled
    }

    override fun getIsVerboseLoggingEnabled(): Boolean {
        return settingsService.getBoolean(KEY_IS_VERBOSE_LOGGING_ENABLED) ?: config.managementGlobalSettings.isVerboseLoggingEnabled
    }

    override suspend fun updateManagementGlobalSettings(
        managementGlobalSettings: ManagementGlobalSettings
    ): AppResult<Unit> {
        val settingsToUpdate = listOf(
            SystemSetting(
                key = KEY_PRIVACY_POLICY,
                value = managementGlobalSettings.privacyPolicyUrl.orEmpty(),
                type = SettingType.STRING
            ),
            SystemSetting(
                key = KEY_TERMS_OF_SERVICE,
                value = managementGlobalSettings.termsOfServiceUrl.orEmpty(),
                type = SettingType.STRING
            ),
            SystemSetting(
                key = KEY_SUPPORT_EMAIL,
                value = managementGlobalSettings.contactSupportEmail.orEmpty(),
                type = SettingType.STRING
            ),
            SystemSetting(
                key = KEY_MIN_SUPPORTED_APP_VERSIONS,
                value = FoundationJson.encodeToString(managementGlobalSettings.minSupportedAppVersions),
                type = SettingType.JSON
            ),
            SystemSetting(
                key = KEY_IS_TRACING_ENABLED,
                value = "${managementGlobalSettings.isTracingEnabled}",
                type = SettingType.BOOLEAN
            ),
            SystemSetting(
                key = KEY_IS_METRICS_ENABLED,
                value = "${managementGlobalSettings.isMetricsEnabled}",
                type = SettingType.BOOLEAN
            ),
            SystemSetting(
                key = KEY_IS_VERBOSE_LOGGING_ENABLED,
                value = "${managementGlobalSettings.isVerboseLoggingEnabled}",
                type = SettingType.BOOLEAN
            )
        )
        return settingsService.updateSettings(settingsToUpdate).mapSuccess { }
    }

    override suspend fun resetManagementGlobalSettings(): AppResult<ManagementGlobalSettings> {
        val defaultSettings = config.managementGlobalSettings
        return updateManagementGlobalSettings(defaultSettings).mapSuccess { defaultSettings }
    }

    private companion object {
        const val KEY_PRIVACY_POLICY = "global.privacy_policy_url"
        const val KEY_TERMS_OF_SERVICE = "global.terms_of_service_url"
        const val KEY_SUPPORT_EMAIL = "global.contact_support_email"
        const val KEY_MIN_SUPPORTED_APP_VERSIONS = "global.min_supported_app_versions"
        const val KEY_IS_TRACING_ENABLED = "global.is_tracing_enabled"
        const val KEY_IS_METRICS_ENABLED = "global.is_metrics_enabled"
        const val KEY_IS_VERBOSE_LOGGING_ENABLED = "global.is_verbose_logging_enabled"
    }
}
