package io.github.mudrichenkoevgeny.backend.core.settings.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.settings.config.envkeys.SettingsEnvKeys
import io.github.mudrichenkoevgeny.backend.core.settings.config.model.GlobalSettingsConfig
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.ManagementGlobalSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [GlobalSettingsConfigFactory] implementation backed by [EnvReader].
 *
 * All keys are defined in [SettingsEnvKeys]. Missing variables are treated as `null` or default values.
 */
@Singleton
class GlobalSettingsConfigFactoryImpl @Inject constructor(
    private val envReader: EnvReader
): GlobalSettingsConfigFactory {

    override fun create(): GlobalSettingsConfig {
        val privacyPolicyUrl = envReader.getByKeyOrNull(SettingsEnvKeys.PRIVACY_POLICY_URL)
        val termsOfServiceUrl = envReader.getByKeyOrNull(SettingsEnvKeys.TERMS_OF_SERVICE_URL)
        val contactSupportEmail = envReader.getByKeyOrNull(SettingsEnvKeys.CONTACT_SUPPORT_EMAIL)

        val minSupportedAppVersionsRaw = envReader.getByKeyOrNull(SettingsEnvKeys.MIN_SUPPORTED_APP_VERSIONS)
        val minSupportedAppVersions = minSupportedAppVersionsRaw?.let { json ->
            try {
                FoundationJson.decodeFromString<Map<ClientType, String>>(json)
            } catch (_: Exception) {
                emptyMap()
            }
        } ?: emptyMap()

        val isTracingEnabled = envReader.getByKeyOrNull(SettingsEnvKeys.IS_TRACING_ENABLED)?.toBooleanStrictOrNull() ?: false
        val isMetricsEnabled = envReader.getByKeyOrNull(SettingsEnvKeys.IS_METRICS_ENABLED)?.toBooleanStrictOrNull() ?: false
        val isVerboseLoggingEnabled = envReader.getByKeyOrNull(SettingsEnvKeys.IS_VERBOSE_LOGGING_ENABLED)?.toBooleanStrictOrNull() ?: false

        val managementGlobalSettings = ManagementGlobalSettings(
            privacyPolicyUrl = privacyPolicyUrl,
            termsOfServiceUrl = termsOfServiceUrl,
            contactSupportEmail = contactSupportEmail,
            maintenanceUntilEpochMillis = null,
            minSupportedAppVersions = minSupportedAppVersions,
            isTracingEnabled = isTracingEnabled,
            isMetricsEnabled = isMetricsEnabled,
            isVerboseLoggingEnabled = isVerboseLoggingEnabled
        )

        return GlobalSettingsConfig(
            managementGlobalSettings = managementGlobalSettings
        )
    }
}
