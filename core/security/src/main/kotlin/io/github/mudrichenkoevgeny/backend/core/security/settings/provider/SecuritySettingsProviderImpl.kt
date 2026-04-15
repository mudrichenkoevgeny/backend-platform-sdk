package io.github.mudrichenkoevgeny.backend.core.security.settings.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.flatMapSuccess
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.SecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.passwordpolicy.toPasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.passwordpolicy.toPasswordPolicyPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.passwordpolicy.PasswordPolicyPayload
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SecuritySettingsProvider] implementation backed by [SystemSettingsService].
 *
 * Stores password policy as JSON and recent-auth validity as a long under dedicated keys. When a key is
 * missing, [getPasswordPolicy] / [getRecentAuthenticationValidityInMinutes] fall back to [SecurityConfig].
 */
@Singleton
class SecuritySettingsProviderImpl @Inject constructor(
    private val settingsService: SystemSettingsService,
    private val config: SecurityConfig
) : SecuritySettingsProvider {

    override suspend fun initialize(): AppResult<Unit> {
        return settingsService.registerDefault(
            key = RECENT_AUTHENTICATION_VALIDITY_IN_MINUTES,
            value = "${config.recentAuthenticationValidityInMinutes}",
            type = SettingType.LONG
        ).flatMapSuccess {
            settingsService.registerDefault(
                key = KEY_PASSWORD_POLICY,
                value = FoundationJson.encodeToString(config.passwordPolicy.toPasswordPolicyPayload()),
                type = SettingType.JSON
            )
        }
    }

    override fun getSettings(): AppResult<SecuritySettings> {
        val settings = SecuritySettings(
            recentAuthenticationValidityInMinutes = getRecentAuthenticationValidityInMinutes(),
            passwordPolicy = getPasswordPolicy()
        )
        return AppResult.Success(settings)
    }

    override fun getRecentAuthenticationValidityInMinutes(): Long {
        return settingsService.getLong(RECENT_AUTHENTICATION_VALIDITY_IN_MINUTES)
            ?: config.recentAuthenticationValidityInMinutes
    }

    override fun getPasswordPolicy(): PasswordPolicy {
        return settingsService.getJson(KEY_PASSWORD_POLICY) { json ->
            FoundationJson.decodeFromString<PasswordPolicyPayload>(json).toPasswordPolicy()
        } ?: config.passwordPolicy
    }

    override suspend fun updateSecuritySettings(securitySettings: SecuritySettings): AppResult<Unit> {
        return settingsService.updateSetting(
            key = RECENT_AUTHENTICATION_VALIDITY_IN_MINUTES,
            value = "${securitySettings.recentAuthenticationValidityInMinutes}",
            type = SettingType.LONG
        ).flatMapSuccess {
            settingsService.updateSetting(
                key = KEY_PASSWORD_POLICY,
                value = FoundationJson.encodeToString(securitySettings.passwordPolicy),
                type = SettingType.JSON
            ).flatMapSuccess { AppResult.Success(Unit) }
        }
    }

    private companion object {
        const val RECENT_AUTHENTICATION_VALIDITY_IN_MINUTES = "security.recent_authentication_validity_in_minutes"
        const val KEY_PASSWORD_POLICY = "security.password_policy"
    }
}