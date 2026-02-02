package io.github.mudrichenkoevgeny.backend.feature.user.config.model

import io.github.mudrichenkoevgeny.backend.core.common.config.seed.AdminAccount
import java.time.Duration

data class UserConfig(
    val jwtSecret: String,
    val accessTokenValidityHours: Long,
    val refreshTokenValidityDays: Long,
    val authRealm: String,
    val adminAccountsList: List<AdminAccount>
) {

    fun getAccessTokenValidityHoursDuration(): Duration = Duration.ofHours(accessTokenValidityHours)

    fun getRefreshTokenValidityDaysDuration(): Duration = Duration.ofDays(refreshTokenValidityDays)
}