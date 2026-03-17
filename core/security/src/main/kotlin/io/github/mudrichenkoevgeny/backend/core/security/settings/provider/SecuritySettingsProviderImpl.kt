package io.github.mudrichenkoevgeny.backend.core.security.settings.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.backend.core.security.settings.model.SecuritySettings
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.model.PasswordPolicy
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SecuritySettingsProvider] implementation backed by [SystemSettingsService].
 *
 * Stores password policy as JSON under a single settings key. When the key is missing, falls back
 * to the default policy from [SecurityConfig].
 */
@Singleton
class SecuritySettingsProviderImpl @Inject constructor(
    private val settingsService: SystemSettingsService,
    private val config: SecurityConfig
) : SecuritySettingsProvider {

    override suspend fun initialize(): AppResult<Unit> {
        val passwordPolicyJson = FoundationJson.encodeToString(config.passwordPolicy)

        return settingsService.registerDefault(
            key = KEY_PASSWORD_POLICY,
            value = passwordPolicyJson,
            type = SettingType.JSON
        )
    }

    override fun getSettings(): AppResult<SecuritySettings> {
        val settings = SecuritySettings(
            passwordPolicy = settingsService.getJson(KEY_PASSWORD_POLICY) { json ->
                FoundationJson.decodeFromString<PasswordPolicy>(json)
            } ?: config.passwordPolicy
        )

        return AppResult.Success(settings)
    }

    override fun requirePasswordPolicy(): PasswordPolicy {
        return settingsService.getJson(KEY_PASSWORD_POLICY) { json ->
            FoundationJson.decodeFromString<PasswordPolicy>(json)
        } ?: config.passwordPolicy
    }

    override suspend fun updatePasswordPolicy(policy: PasswordPolicy): AppResult<Unit> {
        val jsonValue = FoundationJson.encodeToString(policy)
        val result = settingsService.updateSetting(KEY_PASSWORD_POLICY, jsonValue, SettingType.JSON)

        return when (result) {
            is AppResult.Success -> AppResult.Success(Unit)
            is AppResult.Error -> AppResult.Error(result.error)
        }
    }

    private companion object {
        const val KEY_PASSWORD_POLICY = "security.password_policy"
    }
}