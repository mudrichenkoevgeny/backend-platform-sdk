package io.github.mudrichenkoevgeny.backend.feature.user.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.common.config.env.getStringList
import io.github.mudrichenkoevgeny.backend.core.common.config.env.readJsonSecret
import io.github.mudrichenkoevgeny.backend.core.common.config.seed.AdminList
import io.github.mudrichenkoevgeny.backend.feature.user.config.envkeys.UserEnvKeys
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthSettings
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AvailableAuthProviders
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.resend.model.ResendConfig
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model.UniOneConfig
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.UserAuthProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
/**
 * Default [UserConfigFactory] implementation that reads configuration from environment variables
 * and secret files via [EnvReader].
 *
 * The factory:
 * - loads secrets (JWT secret, admin list JSON, provider API keys) from paths specified in env
 * - parses available auth providers from string lists into [UserAuthProvider] values
 * - builds email provider configs ([UniOneConfig], [ResendConfig]) when required values are present
 */
class UserConfigFactoryImpl @Inject constructor(
    private val envReader: EnvReader
): UserConfigFactory {

    override fun create(): UserConfig {
        // secret files
        val jwtSecretFile = envReader.getByKey(UserEnvKeys.JWT_SECRET_FILE)
        val adminAccountsJsonFile = envReader.getByKey(UserEnvKeys.ADMIN_ACCOUNTS_JSON_SECRET_FILE)
        val uniOneApiKeyFile = envReader.getByKey(UserEnvKeys.UNIONE_API_KEY_FILE)

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

        val googleWebClientId = envReader.getByKey(UserEnvKeys.GOOGLE_WEB_CLIENT_ID)

        val uniOneApiKey = envReader.readSecret(uniOneApiKeyFile)
        val uniOneUrl = envReader.getByKey(UserEnvKeys.UNIONE_URL)
        val uniOneFromEmail = envReader.getByKey(UserEnvKeys.UNIONE_FROM_EMAIL)
        val uniOneFromName = envReader.getByKey(UserEnvKeys.UNIONE_FROM_NAME)
        val uniOneTrackDomain = envReader.getByKey(UserEnvKeys.UNIONE_TRACK_DOMAIN)
        val uniOneApiSend = envReader.getByKey(UserEnvKeys.UNIONE_API_SEND)

        val uniOneConfig = UniOneConfig.createOrNull(
            apiKey = uniOneApiKey,
            url = uniOneUrl,
            fromEmail = uniOneFromEmail,
            fromName = uniOneFromName,
            trackDomain = uniOneTrackDomain,
            apiSend = uniOneApiSend
        )

        val resendApiKeyFile = envReader.getByKey(UserEnvKeys.RESEND_API_KEY_FILE)
        val resendApiKey = envReader.readSecret(resendApiKeyFile)
        val resendUrl = envReader.getByKey(UserEnvKeys.RESEND_URL)
        val resendFromEmail = envReader.getByKey(UserEnvKeys.RESEND_FROM_EMAIL)
        val resendFromName = envReader.getByKey(UserEnvKeys.RESEND_FROM_NAME)

        val resendConfig = ResendConfig.createOrNull(
            apiKey = resendApiKey,
            url = resendUrl,
            fromEmail = resendFromEmail,
            fromName = resendFromName
        )

        return UserConfig(
            jwtSecret = jwtSecret,
            accessTokenValidityHours = accessTokenValidityHours,
            refreshTokenValidityDays = refreshTokenValidityDays,
            authRealm = authRealm,
            adminAccountsList = adminList.admins,
            authSettings = authSettings,
            googleWebClientId = googleWebClientId,
            uniOneConfig = uniOneConfig,
            resendConfig = resendConfig
        )
    }
}