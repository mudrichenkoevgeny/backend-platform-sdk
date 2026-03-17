package io.github.mudrichenkoevgeny.backend.core.settings.config.model

/**
 * Static settings configuration loaded from the environment at application startup.
 *
 * This config acts as a seed for DB-backed settings (see `global` settings provider) and is typically
 * created by `SettingsConfigFactory`.
 *
 * @property privacyPolicyUrl optional URL to a privacy policy page
 * @property termsOfServiceUrl optional URL to a terms of service page
 * @property contactSupportEmail optional support email address exposed to clients
 */
data class SettingsConfig(
    val privacyPolicyUrl: String?,
    val termsOfServiceUrl: String?,
    val contactSupportEmail: String?
)