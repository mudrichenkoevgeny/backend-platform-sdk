package io.github.mudrichenkoevgeny.backend.core.settings.config.envkeys

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.settings.config.factory.GlobalSettingsConfigFactory

/**
 * Environment variable keys used by the settings module.
 *
 * The actual values are read by [EnvReader] via [GlobalSettingsConfigFactory].
 */
public object SettingsEnvKeys {
    public const val PRIVACY_POLICY_URL: String = "PRIVACY_POLICY_URL"
    public const val TERMS_OF_SERVICE_URL: String = "TERMS_OF_SERVICE_URL"
    public const val CONTACT_SUPPORT_EMAIL: String = "CONTACT_SUPPORT_EMAIL"
    public const val MIN_SUPPORTED_APP_VERSIONS: String = "MIN_SUPPORTED_APP_VERSIONS"
    public const val IS_TRACING_ENABLED: String = "IS_TRACING_ENABLED"
    public const val IS_METRICS_ENABLED: String = "IS_METRICS_ENABLED"
    public const val IS_VERBOSE_LOGGING_ENABLED: String = "IS_VERBOSE_LOGGING_ENABLED"
}
