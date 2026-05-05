package io.github.mudrichenkoevgeny.backend.feature.user.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.common.config.env.getStringList
import io.github.mudrichenkoevgeny.backend.core.common.config.env.readJsonSecret
import io.github.mudrichenkoevgeny.backend.feature.user.config.seed.AdminList
import io.github.mudrichenkoevgeny.backend.feature.user.config.envkeys.UserEnvKeys
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.resend.model.ResendConfig
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model.UniOneConfig
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.AvailableAuthProviders
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [UserConfigFactory] implementation that reads configuration from environment variables
 * and secret files via [EnvReader].
 *
 * The factory:
 * - loads secrets (JWT secret, admin list JSON, provider API keys) from paths specified in env
 * - parses available auth providers from string lists into [UserAuthProvider] values and builds [ManagementAuthSettings]
 * - builds email provider configs ([UniOneConfig], [ResendConfig]) when required values are present
 */
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
        val adminList: AdminList = envReader.readJsonSecret(adminAccountsJsonFile)

        val availablePrimaryAuthProviders = envReader
            .getStringList(UserEnvKeys.AVAILABLE_AUTH_PROVIDERS_PRIMARY)
            .mapNotNull { UserAuthProvider.fromValueOrNull(it) }
        val availableSecondaryAuthProviders = envReader
            .getStringList(UserEnvKeys.AVAILABLE_AUTH_PROVIDERS_SECONDARY)
            .mapNotNull { UserAuthProvider.fromValueOrNull(it) }
        val maxTotalIdentifiers = envReader.getByKey(UserEnvKeys.MAX_TOTAL_IDENTIFIERS).toInt()
        val maxEmailIdentifiers = envReader.getByKey(UserEnvKeys.MAX_EMAIL_IDENTIFIERS).toInt()
        val maxPhoneIdentifiers = envReader.getByKey(UserEnvKeys.MAX_PHONE_IDENTIFIERS).toInt()
        val maxIdentifiersPerExternalProvider = envReader.getByKey(UserEnvKeys.MAX_IDENTIFIERS_PER_EXTERNAL_PROVIDER).toInt()
        val maxActiveSessions = envReader.getByKey(UserEnvKeys.MAX_ACTIVE_SESSIONS).toInt()
        val accessTokenExpirationSeconds = envReader.getByKey(UserEnvKeys.ACCESS_TOKEN_EXPIRATION_SECONDS).toInt()
        val refreshTokenExpirationSeconds = envReader.getByKey(UserEnvKeys.REFRESH_TOKEN_EXPIRATION_SECONDS).toInt()
        val accountDeletionDelaySeconds = envReader.getByKey(UserEnvKeys.ACCOUNT_DELETION_DELAY_SECONDS).toInt()
        val managementAuthSettings = ManagementAuthSettings(
            availableAuthProviders = AvailableAuthProviders(
                primary = availablePrimaryAuthProviders,
                secondary = availableSecondaryAuthProviders
            ),
            maxTotalIdentifiers = maxTotalIdentifiers,
            maxEmailIdentifiers = maxEmailIdentifiers,
            maxPhoneIdentifiers = maxPhoneIdentifiers,
            maxIdentifiersPerExternalProvider = maxIdentifiersPerExternalProvider,
            maxActiveSessions = maxActiveSessions,
            accessTokenExpirationSeconds = accessTokenExpirationSeconds,
            refreshTokenExpirationSeconds = refreshTokenExpirationSeconds,
            accountDeletionDelaySeconds = accountDeletionDelaySeconds
        )

        // Auth Service Google
        val googleWebClientId = envReader.getByKey(UserEnvKeys.GOOGLE_WEB_CLIENT_ID)

        // Email Service Unione
        val uniOneApiKeyFile = envReader.getByKeyOrNull(UserEnvKeys.UNIONE_API_KEY_FILE)
        val uniOneConfig = if (!uniOneApiKeyFile.isNullOrBlank()) {
            val uniOneApiKey = envReader.readSecret(uniOneApiKeyFile)
            UniOneConfig.createOrNull(
                apiKey = uniOneApiKey,
                url = envReader.getByKey(UserEnvKeys.UNIONE_URL),
                fromEmail = envReader.getByKey(UserEnvKeys.UNIONE_FROM_EMAIL),
                fromName = envReader.getByKey(UserEnvKeys.UNIONE_FROM_NAME),
                trackDomain = envReader.getByKey(UserEnvKeys.UNIONE_TRACK_DOMAIN),
                apiSend = envReader.getByKey(UserEnvKeys.UNIONE_API_SEND)
            )
        } else {
            null
        }

        // Email Service Resend
        val resendApiKeyFile = envReader.getByKeyOrNull(UserEnvKeys.RESEND_API_KEY_FILE)
        val resendConfig = if (!resendApiKeyFile.isNullOrBlank()) {
            ResendConfig.createOrNull(
                apiKey = envReader.readSecret(resendApiKeyFile),
                url = envReader.getByKey(UserEnvKeys.RESEND_URL),
                fromEmail = envReader.getByKey(UserEnvKeys.RESEND_FROM_EMAIL),
                fromName = envReader.getByKey(UserEnvKeys.RESEND_FROM_NAME)
            )
        } else {
            null
        }

        return UserConfig(
            jwtSecret = jwtSecret,
            adminAccountsList = adminList.admins,
            managementAuthSettings = managementAuthSettings,
            googleWebClientId = googleWebClientId,
            uniOneConfig = uniOneConfig,
            resendConfig = resendConfig
        )
    }
}