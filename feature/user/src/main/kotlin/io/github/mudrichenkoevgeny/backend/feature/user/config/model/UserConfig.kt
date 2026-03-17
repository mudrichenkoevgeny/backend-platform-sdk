package io.github.mudrichenkoevgeny.backend.feature.user.config.model

import io.github.mudrichenkoevgeny.backend.core.common.config.seed.AdminAccount
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthSettings
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.resend.model.ResendConfig
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model.UniOneConfig
import java.time.Duration

/**
 * Configuration for the user feature.
 *
 * This config is intended to be created at application startup (typically via a config factory)
 * and injected into feature components that need auth, seeding, and external provider settings.
 *
 * @param jwtSecret secret used to sign and verify JWT tokens
 * @param accessTokenValidityHours access token validity in hours
 * @param refreshTokenValidityDays refresh token validity in days
 * @param authRealm authentication realm used by the server
 * @param adminAccountsList initial admin accounts to seed on startup
 * @param authSettings feature auth settings (e.g. available auth providers)
 * @param googleWebClientId optional Google web client id used to verify Google tokens
 * @param uniOneConfig optional UniOne email provider config (null when not configured)
 * @param resendConfig optional Resend email provider config (null when not configured)
 */
data class UserConfig(
    val jwtSecret: String,
    val accessTokenValidityHours: Long,
    val refreshTokenValidityDays: Long,
    val authRealm: String,
    val adminAccountsList: List<AdminAccount>,
    val authSettings: AuthSettings,
    val googleWebClientId: String?,
    val uniOneConfig: UniOneConfig?,
    val resendConfig: ResendConfig?
) {

    /**
     * Access token validity as a [Duration].
     */
    fun getAccessTokenValidityHoursDuration(): Duration = Duration.ofHours(accessTokenValidityHours)

    /**
     * Refresh token validity as a [Duration].
     */
    fun getRefreshTokenValidityDaysDuration(): Duration = Duration.ofDays(refreshTokenValidityDays)
}