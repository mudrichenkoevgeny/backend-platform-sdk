package io.github.mudrichenkoevgeny.backend.core.security.settings.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.flatMapSuccess
import io.github.mudrichenkoevgeny.backend.core.common.result.mapSuccess
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.SecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.otpconfirmation.toOtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.otpconfirmation.toOtpConfirmationPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.passwordpolicy.toPasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.passwordpolicy.toPasswordPolicyPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.otpconfirmation.OtpConfirmationPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.passwordpolicy.PasswordPolicyPayload
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [SecuritySettingsProvider] implementation backed by [SystemSettingsService].
 */
@Singleton
class SecuritySettingsProviderImpl @Inject constructor(
    private val settingsService: SystemSettingsService,
    private val config: SecurityConfig
) : SecuritySettingsProvider {

    override suspend fun initialize(): AppResult<Unit> {
        return settingsService.registerDefault(
            key = KEY_RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS,
            value = "${config.recentAuthenticationValidityInSeconds}",
            type = SettingType.INT
        ).flatMapSuccess {
            settingsService.registerDefault(
                key = KEY_RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS_FOR_MANAGEMENT,
                value = "${config.recentAuthenticationValidityInSecondsForManagement}",
                type = SettingType.INT
            )
        }.flatMapSuccess {
            settingsService.registerDefault(
                key = KEY_PASSWORD_POLICY,
                value = FoundationJson.encodeToString(config.passwordPolicy.toPasswordPolicyPayload()),
                type = SettingType.JSON
            )
        }.flatMapSuccess {
            settingsService.registerDefault(
                key = KEY_OTP_CONFIRMATION,
                value = FoundationJson.encodeToString(config.otpConfirmation.toOtpConfirmationPayload()),
                type = SettingType.JSON
            )
        }.flatMapSuccess {
            settingsService.registerDefault(
                key = KEY_MFA_TOKEN_EXPIRATION_SECONDS,
                value = "${config.mfaTokenExpirationSeconds}",
                type = SettingType.INT
            )
        }
    }

    override fun getSettings(): SecuritySettings {
        return SecuritySettings(
            recentAuthenticationValiditySeconds = getRecentAuthenticationValidityInSeconds(),
            recentAuthenticationValiditySecondsForManagement = getRecentAuthenticationValidityInSecondsForManagement(),
            passwordPolicy = getPasswordPolicy(),
            otpConfirmation = getOtpConfirmation(),
            mfaTokenExpirationSeconds = getMfaTokenExpirationSeconds()
        )
    }

    override fun getRecentAuthenticationValidityInSeconds(): Int {
        return settingsService.getLong(KEY_RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS)?.toInt()
            ?: config.recentAuthenticationValidityInSeconds
    }

    override fun getRecentAuthenticationValidityInSecondsForManagement(): Int {
        return settingsService.getLong(KEY_RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS_FOR_MANAGEMENT)?.toInt()
            ?: config.recentAuthenticationValidityInSecondsForManagement
    }

    override fun getPasswordPolicy(): PasswordPolicy {
        return settingsService.getJson(KEY_PASSWORD_POLICY) { json ->
            FoundationJson.decodeFromString<PasswordPolicyPayload>(json).toPasswordPolicy()
        } ?: config.passwordPolicy
    }

    override fun getOtpConfirmation(): OtpConfirmation {
        return settingsService.getJson(KEY_OTP_CONFIRMATION) { json ->
            FoundationJson.decodeFromString<OtpConfirmationPayload>(json).toOtpConfirmation()
        } ?: config.otpConfirmation
    }

    override fun getMfaTokenExpirationSeconds(): Int {
        return settingsService.getLong(KEY_MFA_TOKEN_EXPIRATION_SECONDS)?.toInt()
            ?: config.mfaTokenExpirationSeconds
    }

    override suspend fun updateSecuritySettings(securitySettings: SecuritySettings): AppResult<Unit> {
        return settingsService.updateSetting(
            key = KEY_RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS,
            value = "${securitySettings.recentAuthenticationValiditySeconds}",
            type = SettingType.INT
        ).flatMapSuccess {
            settingsService.updateSetting(
                key = KEY_RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS_FOR_MANAGEMENT,
                value = "${securitySettings.recentAuthenticationValiditySecondsForManagement}",
                type = SettingType.INT
            )
        }.flatMapSuccess {
            settingsService.updateSetting(
                key = KEY_PASSWORD_POLICY,
                value = FoundationJson.encodeToString(securitySettings.passwordPolicy.toPasswordPolicyPayload()),
                type = SettingType.JSON
            )
        }.flatMapSuccess {
            settingsService.updateSetting(
                key = KEY_OTP_CONFIRMATION,
                value = FoundationJson.encodeToString(securitySettings.otpConfirmation.toOtpConfirmationPayload()),
                type = SettingType.JSON
            )
        }.flatMapSuccess {
            settingsService.updateSetting(
                key = KEY_MFA_TOKEN_EXPIRATION_SECONDS,
                value = "${securitySettings.mfaTokenExpirationSeconds}",
                type = SettingType.INT
            )
        }.mapSuccess { }
    }

    private companion object {
        const val KEY_RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS = "security.recent_authentication_validity_in_seconds"
        const val KEY_RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS_FOR_MANAGEMENT = "security.recent_authentication_validity_in_seconds_for_management"
        const val KEY_PASSWORD_POLICY = "security.password_policy"
        const val KEY_OTP_CONFIRMATION = "security.otp_confirmation"
        const val KEY_MFA_TOKEN_EXPIRATION_SECONDS = "security.mfa_token_expiration_seconds"
    }
}