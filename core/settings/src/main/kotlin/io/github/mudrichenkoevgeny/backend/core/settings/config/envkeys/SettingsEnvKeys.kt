package io.github.mudrichenkoevgeny.backend.core.settings.config.envkeys

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.settings.config.factory.GlobalSettingsConfigFactory

/**
 * Environment variable keys used by the settings module.
 *
 * The actual values are read by [EnvReader] via [GlobalSettingsConfigFactory].
 */
object SettingsEnvKeys {
    const val PRIVACY_POLICY_URL = "PRIVACY_POLICY_URL"
    const val TERMS_OF_SERVICE_URL = "TERMS_OF_SERVICE_URL"
    const val CONTACT_SUPPORT_EMAIL = "CONTACT_SUPPORT_EMAIL"
    const val MIN_SUPPORTED_APP_VERSIONS = "MIN_SUPPORTED_APP_VERSIONS"
    const val IS_TRACING_ENABLED = "IS_TRACING_ENABLED"
    const val IS_METRICS_ENABLED = "IS_METRICS_ENABLED"
    const val IS_VERBOSE_LOGGING_ENABLED = "IS_VERBOSE_LOGGING_ENABLED"
}