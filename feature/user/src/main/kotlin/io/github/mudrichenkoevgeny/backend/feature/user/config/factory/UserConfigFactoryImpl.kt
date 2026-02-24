package io.github.mudrichenkoevgeny.backend.feature.user.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.common.config.env.getStringList
import io.github.mudrichenkoevgeny.backend.core.common.config.env.readJsonSecret
import io.github.mudrichenkoevgeny.backend.core.common.config.seed.AdminList
import io.github.mudrichenkoevgeny.backend.feature.user.config.envkeys.UserEnvKeys
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthSettings
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AvailableAuthProviders
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserConfigFactoryImpl @Inject constructor(
    private val envReader: EnvReader
): UserConfigFactory {

    override fun create(): UserConfig {
        // secret files
        val jwtSecretFile = envReader.getByKey(UserEnvKeys.JWT_SECRET_FILE)
        val adminAccountsJsonFile = envReader.getByKey(UserEnvKeys.ADMIN_ACCOUNTS_JSON_SECRET_FILE)

        // env
        val jwtSecret = envReader.readSecret(jwtSecretFile)
        val accessTokenValidityHours = envReader.getByKey(UserEnvKeys.ACCESS_TOKEN_VALIDITY_HOURS).toLong()
        val refreshTokenValidityDays = envReader.getByKey(UserEnvKeys.REFRESH_TOKEN_VALIDITY_DAYS).toLong()
        val authRealm = envReader.getByKey(UserEnvKeys.AUTH_REALM)
        val adminList: AdminList = envReader.readJsonSecret(adminAccountsJsonFile)

        val availablePrimaryAuthProviders = envReader
            .getStringList(UserEnvKeys.AVAILABLE_AUTH_PROVIDERS_PRIMARY)
            .mapNotNull { UserAuthProvider.fromValue(it) }
        val availableSecondaryAuthProviders = envReader
            .getStringList(UserEnvKeys.AVAILABLE_AUTH_PROVIDERS_SECONDARY)
            .mapNotNull { UserAuthProvider.fromValue(it) }
        val availableAuthProviders = AvailableAuthProviders(
            primary = availablePrimaryAuthProviders,
            secondary = availableSecondaryAuthProviders
        )
        val authSettings = AuthSettings(
            availableAuthProviders = availableAuthProviders
        )

        return UserConfig(
            jwtSecret = jwtSecret,
            accessTokenValidityHours = accessTokenValidityHours,
            refreshTokenValidityDays = refreshTokenValidityDays,
            authRealm = authRealm,
            adminAccountsList = adminList.admins,
            authSettings = authSettings
        )
    }
}