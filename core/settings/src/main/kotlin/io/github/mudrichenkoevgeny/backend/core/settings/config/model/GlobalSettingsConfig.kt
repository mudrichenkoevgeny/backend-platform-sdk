package io.github.mudrichenkoevgeny.backend.core.settings.config.model

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType

/**
 * Static settings configuration loaded from the environment at application startup.
 *
 * This config acts as a seed for DB-backed settings (see `global` settings provider) and is typically
 * created by `GlobalSettingsConfigFactory`.
 *
 * @property privacyPolicyUrl optional URL to a privacy policy page
 * @property termsOfServiceUrl optional URL to a terms of service page
 * @property contactSupportEmail optional support email address exposed to clients
 * @property minSupportedAppVersions minimum supported application versions mapped by client type
 * @property isTracingEnabled global flag indicating whether tracing is enabled
 * @property isMetricsEnabled global flag indicating whether metrics collection is enabled
 * @property isVerboseLoggingEnabled flag controlling logging verbosity
 */
data class GlobalSettingsConfig(
    val privacyPolicyUrl: String?,
    val termsOfServiceUrl: String?,
    val contactSupportEmail: String?,
    val minSupportedAppVersions: Map<ClientType, String> = emptyMap(),
    val isTracingEnabled: Boolean = false,
    val isMetricsEnabled: Boolean = false,
    val isVerboseLoggingEnabled: Boolean = false
)