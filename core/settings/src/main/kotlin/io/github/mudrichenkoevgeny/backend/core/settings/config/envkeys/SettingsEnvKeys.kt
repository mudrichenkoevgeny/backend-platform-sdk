package io.github.mudrichenkoevgeny.backend.core.settings.config.envkeys

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.settings.config.factory.SettingsConfigFactory

/**
 * Environment variable keys used by the settings module.
 *
 * The actual values are read by [EnvReader] via [SettingsConfigFactory].
 */
object SettingsEnvKeys {
    const val PRIVACY_POLICY_URL = "PRIVACY_POLICY_URL"
    const val TERMS_OF_SERVICE_URL = "TERMS_OF_SERVICE_URL"
    const val CONTACT_SUPPORT_EMAIL = "CONTACT_SUPPORT_EMAIL"
}