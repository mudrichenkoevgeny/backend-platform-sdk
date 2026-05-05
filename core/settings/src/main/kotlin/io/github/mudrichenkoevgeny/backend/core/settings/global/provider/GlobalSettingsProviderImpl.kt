package io.github.mudrichenkoevgeny.backend.core.settings.global.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.flatMapSuccess
import io.github.mudrichenkoevgeny.backend.core.common.result.mapSuccess
import io.github.mudrichenkoevgeny.backend.core.settings.config.model.SettingsConfig
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.GlobalSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalSettingsProviderImpl @Inject constructor(
    private val settingsService: SystemSettingsService,
    private val config: SettingsConfig
) : GlobalSettingsProvider {

    override suspend fun initialize(): AppResult<Unit> {
        return settingsService.registerDefault(
            key = KEY_PRIVACY_POLICY,
            value = config.privacyPolicyUrl.orEmpty(),
            type = SettingType.STRING
        ).flatMapSuccess {
            settingsService.registerDefault(
                key = KEY_TERMS_OF_SERVICE,
                value = config.termsOfServiceUrl.orEmpty(),
                type = SettingType.STRING
            )
        }.flatMapSuccess {
            settingsService.registerDefault(
                key = KEY_SUPPORT_EMAIL,
                value = config.contactSupportEmail.orEmpty(),
                type = SettingType.STRING
            )
        }
    }

    override fun getSettings(): GlobalSettings {
        return GlobalSettings(
            privacyPolicyUrl = getPrivacyPolicyUrl(),
            termsOfServiceUrl = getTermsOfServiceUrl(),
            contactSupportEmail = getContactSupportEmail()
        )
    }

    override fun getPrivacyPolicyUrl(): String? {
        return settingsService.getString(KEY_PRIVACY_POLICY) ?: config.privacyPolicyUrl
    }

    override fun getTermsOfServiceUrl(): String? {
        return settingsService.getString(KEY_TERMS_OF_SERVICE) ?: config.termsOfServiceUrl
    }

    override fun getContactSupportEmail(): String? {
        return settingsService.getString(KEY_SUPPORT_EMAIL) ?: config.contactSupportEmail
    }

    override suspend fun updateGlobalSettings(globalSettings: GlobalSettings): AppResult<Unit> {
        return settingsService.updateSetting(
            key = KEY_PRIVACY_POLICY,
            value = globalSettings.privacyPolicyUrl.orEmpty(),
            type = SettingType.STRING
        ).flatMapSuccess {
            settingsService.updateSetting(
                key = KEY_TERMS_OF_SERVICE,
                value = globalSettings.termsOfServiceUrl.orEmpty(),
                type = SettingType.STRING
            )
        }.flatMapSuccess {
            settingsService.updateSetting(
                key = KEY_SUPPORT_EMAIL,
                value = globalSettings.contactSupportEmail.orEmpty(),
                type = SettingType.STRING
            )
        }.mapSuccess { }
    }

    private companion object {
        const val KEY_PRIVACY_POLICY = "global.privacy_policy_url"
        const val KEY_TERMS_OF_SERVICE = "global.terms_of_service_url"
        const val KEY_SUPPORT_EMAIL = "global.contact_support_email"
    }
}