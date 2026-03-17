package io.github.mudrichenkoevgeny.backend.core.settings.global.model

/**
 * Public global settings exposed to clients.
 *
 * Values are typically backed by DB-stored system settings and can be seeded from environment
 * configuration on startup.
 */
data class GlobalSettings(
    val privacyPolicyUrl: String?,
    val termsOfServiceUrl: String?,
    val contactSupportEmail: String?
)