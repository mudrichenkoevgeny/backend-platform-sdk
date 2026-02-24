package io.github.mudrichenkoevgeny.backend.core.settings.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.settings.config.envkeys.SettingsEnvKeys
import io.github.mudrichenkoevgeny.backend.core.settings.config.model.SettingsConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsConfigFactoryImpl @Inject constructor(
    private val envReader: EnvReader
): SettingsConfigFactory {

    override fun create(): SettingsConfig {
        val privacyPolicyUrl = envReader.getByKeyOrNull(SettingsEnvKeys.PRIVACY_POLICY_URL)
        val termsOfServiceUrl = envReader.getByKeyOrNull(SettingsEnvKeys.TERMS_OF_SERVICE_URL)
        val contactSupportEmail = envReader.getByKeyOrNull(SettingsEnvKeys.CONTACT_SUPPORT_EMAIL)

        return SettingsConfig(
            privacyPolicyUrl = privacyPolicyUrl,
            termsOfServiceUrl = termsOfServiceUrl,
            contactSupportEmail = contactSupportEmail
        )
    }
}