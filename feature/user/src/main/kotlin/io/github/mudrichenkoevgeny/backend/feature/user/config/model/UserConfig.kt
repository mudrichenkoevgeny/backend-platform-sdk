package io.github.mudrichenkoevgeny.backend.feature.user.config.model

import io.github.mudrichenkoevgeny.backend.core.common.config.seed.AdminAccount
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthSettings
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.resend.model.ResendConfig
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model.UniOneConfig
import java.time.Duration

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

    fun getAccessTokenValidityHoursDuration(): Duration = Duration.ofHours(accessTokenValidityHours)

    fun getRefreshTokenValidityDaysDuration(): Duration = Duration.ofDays(refreshTokenValidityDays)
}