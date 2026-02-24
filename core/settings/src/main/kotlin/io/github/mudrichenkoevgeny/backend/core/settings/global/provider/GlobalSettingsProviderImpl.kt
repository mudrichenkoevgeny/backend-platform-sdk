package io.github.mudrichenkoevgeny.backend.core.settings.global.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.config.model.SettingsConfig
import io.github.mudrichenkoevgeny.backend.core.settings.global.model.GlobalSettings
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalSettingsProviderImpl @Inject constructor(
    private val settingsService: SystemSettingsService,
    private val config: SettingsConfig
) : GlobalSettingsProvider {

    override suspend fun initialize(): AppResult<Unit> {
        config.privacyPolicyUrl?.let {
            settingsService.registerDefault(KEY_PRIVACY_POLICY, it, SettingType.STRING)
        }
        config.termsOfServiceUrl?.let {
            settingsService.registerDefault(KEY_TERMS_OF_SERVICE, it, SettingType.STRING)
        }
        config.contactSupportEmail?.let {
            settingsService.registerDefault(KEY_SUPPORT_EMAIL, it, SettingType.STRING)
        }
        return AppResult.Success(Unit)
    }

    override fun getSettings(): AppResult<GlobalSettings> {
        val settings = GlobalSettings(
            privacyPolicyUrl = settingsService.getString(KEY_PRIVACY_POLICY),
            termsOfServiceUrl = settingsService.getString(KEY_TERMS_OF_SERVICE),
            contactSupportEmail = settingsService.getString(KEY_SUPPORT_EMAIL)
        )
        return AppResult.Success(settings)
    }

    override suspend fun updatePrivacyPolicyUrl(url: String): AppResult<Unit> {
        val result = settingsService.updateSetting(KEY_PRIVACY_POLICY, url, SettingType.STRING)

        return when (result) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Error -> AppResult.Error(result.error)
        }
    }

    override suspend fun updateTermsOfServiceUrl(url: String): AppResult<Unit> {
        val result = settingsService.updateSetting(KEY_TERMS_OF_SERVICE, url, SettingType.STRING)

        return when (result) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Error -> AppResult.Error(result.error)
        }
    }

    override suspend fun updateContactSupportEmail(email: String): AppResult<Unit> {
        val result = settingsService.updateSetting(KEY_SUPPORT_EMAIL, email, SettingType.STRING)

        return when (result) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Error -> AppResult.Error(result.error)
        }
    }

    private companion object {
        const val KEY_PRIVACY_POLICY = "global.privacy_policy_url"
        const val KEY_TERMS_OF_SERVICE = "global.terms_of_service_url"
        const val KEY_SUPPORT_EMAIL = "global.contact_support_email"
    }
}