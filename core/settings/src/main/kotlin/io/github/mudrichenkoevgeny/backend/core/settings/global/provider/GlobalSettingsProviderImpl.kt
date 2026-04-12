package io.github.mudrichenkoevgeny.backend.core.settings.global.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.flatMapSuccess
import io.github.mudrichenkoevgeny.backend.core.settings.config.model.SettingsConfig
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.GlobalSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [GlobalSettingsProvider] implementation backed by [SystemSettingsService].
 *
 * Seeds optional URL/email defaults from [SettingsConfig] on [initialize]. [getSettings] reads the
 * effective string values from the settings cache.
 */
@Singleton
class GlobalSettingsProviderImpl @Inject constructor(
    private val settingsService: SystemSettingsService,
    private val config: SettingsConfig
) : GlobalSettingsProvider {

    override suspend fun initialize(): AppResult<Unit> {
        return AppResult.Success(Unit).flatMapSuccess {
            if (config.privacyPolicyUrl != null) {
                settingsService.registerDefault(
                    key = KEY_PRIVACY_POLICY,
                    value = config.privacyPolicyUrl,
                    type = SettingType.STRING,
                )
            } else {
                AppResult.Success(Unit)
            }
        }.flatMapSuccess {
            if (config.termsOfServiceUrl != null) {
                settingsService.registerDefault(
                    key = KEY_TERMS_OF_SERVICE,
                    value = config.termsOfServiceUrl,
                    type = SettingType.STRING,
                )
            } else {
                AppResult.Success(Unit)
            }
        }.flatMapSuccess {
            if (config.contactSupportEmail != null) {
                settingsService.registerDefault(
                    key = KEY_SUPPORT_EMAIL,
                    value = config.contactSupportEmail,
                    type = SettingType.STRING,
                )
            } else {
                AppResult.Success(Unit)
            }
        }
    }

    override fun getSettings(): AppResult<GlobalSettings> {
        val settings = GlobalSettings(
            privacyPolicyUrl = settingsService.getString(KEY_PRIVACY_POLICY),
            termsOfServiceUrl = settingsService.getString(KEY_TERMS_OF_SERVICE),
            contactSupportEmail = settingsService.getString(KEY_SUPPORT_EMAIL)
        )
        return AppResult.Success(settings)
    }

    override suspend fun updateGlobalSettings(globalSettings: GlobalSettings): AppResult<Unit> {
        return settingsService.updateSetting(
            key = KEY_PRIVACY_POLICY,
            value = globalSettings.privacyPolicyUrl.orEmpty(),
            type = SettingType.STRING,
        ).flatMapSuccess {
            settingsService.updateSetting(
                key = KEY_TERMS_OF_SERVICE,
                value = globalSettings.termsOfServiceUrl.orEmpty(),
                type = SettingType.STRING,
            )
        }.flatMapSuccess {
            settingsService.updateSetting(
                key = KEY_SUPPORT_EMAIL,
                value = globalSettings.contactSupportEmail.orEmpty(),
                type = SettingType.STRING,
            )
        }.flatMapSuccess { AppResult.Success(Unit) }
    }

    private companion object {
        const val KEY_PRIVACY_POLICY = "global.privacy_policy_url"
        const val KEY_TERMS_OF_SERVICE = "global.terms_of_service_url"
        const val KEY_SUPPORT_EMAIL = "global.contact_support_email"
    }
}
