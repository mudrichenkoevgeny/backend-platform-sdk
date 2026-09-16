package io.github.mudrichenkoevgeny.backend.feature.user.config.model

import io.github.mudrichenkoevgeny.backend.feature.user.config.seed.AdminAccount
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.resend.model.ResendConfig
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model.UniOneConfig

/**
 * Configuration for the user feature.
 *
 * This config is intended to be created at application startup (typically via a config factory)
 * and injected into feature components that need auth, seeding, and external provider settings.
 *
 * @param jwtSecret secret used to sign and verify JWT tokens
 * @param adminAccountsList initial admin accounts to seed on startup
 * @param managementAuthSettings default auth settings: available providers and access/refresh token lifetimes
 * @param googleWebClientId optional Google web client id used to verify Google tokens
 * @param uniOneConfig optional UniOne email provider config (null when not configured)
 * @param resendConfig optional Resend email provider config (null when not configured)
 */
data class UserConfig(
    val jwtSecret: String,
    val adminAccountsList: List<AdminAccount>,
    val managementAuthSettings: ManagementAuthSettings,
    val googleWebClientId: String?,
    val uniOneConfig: UniOneConfig?,
    val resendConfig: ResendConfig?
)