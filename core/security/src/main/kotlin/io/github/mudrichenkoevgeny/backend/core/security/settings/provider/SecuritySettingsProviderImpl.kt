package io.github.mudrichenkoevgeny.backend.core.security.settings.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapSuccess
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.backend.core.settings.model.SettingType
import io.github.mudrichenkoevgeny.backend.core.settings.model.SystemSetting
import io.github.mudrichenkoevgeny.backend.core.settings.service.SystemSettingsService
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.iprestriction.IpRestrictionPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.ManagementPasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.OpenPasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.ManagementSecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.OpenSecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.accountlockout.toAccountLockoutPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.accountlockout.toAccountLockoutPolicyPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.iprestriction.toIpRestrictionPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.iprestriction.toIpRestrictionPolicyPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.otpconfirmation.toOtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.otpconfirmation.toOtpConfirmationPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.passwordpolicy.toManagementPasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.passwordpolicy.toManagementPasswordPolicyPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.accountlockout.AccountLockoutPolicyPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.iprestriction.IpRestrictionPolicyPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.otpconfirmation.OtpConfirmationPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.passwordpolicy.ManagementPasswordPolicyPayload
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [SecuritySettingsProvider] implementation backed by [SystemSettingsService].
 */
@Singleton
class SecuritySettingsProviderImpl @Inject constructor(
    private val settingsService: SystemSettingsService,
    private val config: SecurityConfig
) : SecuritySettingsProvider {

    private val openIpRestrictionPolicyCache = AtomicReference<Pair<String, IpRestrictionPolicy>?>()
    private val managementIpRestrictionPolicyCache = AtomicReference<Pair<String, IpRestrictionPolicy>?>()

    override suspend fun initialize(): AppResult<Unit> {
        val defaultSettings = listOf(
            SystemSetting(
                key = KEY_RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS,
                value = "${config.managementSecuritySettings.recentAuthenticationValiditySecondsForOpenUser}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS_FOR_MANAGEMENT,
                value = "${config.managementSecuritySettings.recentAuthenticationValiditySecondsForManagementUser}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_PASSWORD_POLICY,
                value = FoundationJson.encodeToString(config.managementSecuritySettings.passwordPolicy.toManagementPasswordPolicyPayload()),
                type = SettingType.JSON
            ),
            SystemSetting(
                key = KEY_OTP_CONFIRMATION,
                value = FoundationJson.encodeToString(config.managementSecuritySettings.otpConfirmation.toOtpConfirmationPayload()),
                type = SettingType.JSON
            ),
            SystemSetting(
                key = KEY_ACCOUNT_LOCKOUT_POLICY,
                value = FoundationJson.encodeToString(config.managementSecuritySettings.accountLockoutPolicy.toAccountLockoutPolicyPayload()),
                type = SettingType.JSON
            ),
            SystemSetting(
                key = KEY_ACCOUNT_LOCKOUT_CHECK_INTERVAL_SECONDS,
                value = "${config.managementSecuritySettings.accountLockoutCheckIntervalSeconds}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_OPEN_IP_RESTRICTION_POLICY,
                value = FoundationJson.encodeToString(config.managementSecuritySettings.openIpRestrictionPolicy.toIpRestrictionPolicyPayload()),
                type = SettingType.JSON
            ),
            SystemSetting(
                key = KEY_MANAGEMENT_IP_RESTRICTION_POLICY,
                value = FoundationJson.encodeToString(config.managementSecuritySettings.managementIpRestrictionPolicy.toIpRestrictionPolicyPayload()),
                type = SettingType.JSON
            ),
            SystemSetting(
                key = KEY_MFA_TOKEN_EXPIRATION_SECONDS,
                value = "${config.managementSecuritySettings.mfaTokenExpirationSeconds}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_MAX_REQUESTS_PER_PERIOD,
                value = "${config.managementSecuritySettings.maxRequestsPerPeriod}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_RATE_LIMIT_PERIOD_SECONDS,
                value = "${config.managementSecuritySettings.rateLimitPeriodSeconds}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_REFRESH_TOKEN_ROTATION_GRACE_PERIOD_SECONDS,
                value = "${config.managementSecuritySettings.refreshTokenRotationGracePeriodSeconds}",
                type = SettingType.INT
            )
        )
        return settingsService.registerDefaults(defaultSettings)
    }

    override fun getManagementSecuritySettings(): ManagementSecuritySettings {
        return ManagementSecuritySettings(
            recentAuthenticationValiditySecondsForOpenUser = getRecentAuthenticationValidityInSeconds(),
            recentAuthenticationValiditySecondsForManagementUser = getRecentAuthenticationValidityInSecondsForManagement(),
            passwordPolicy = getManagementPasswordPolicy(),
            otpConfirmation = getOtpConfirmation(),
            accountLockoutPolicy = getAccountLockoutPolicy(),
            accountLockoutCheckIntervalSeconds = getAccountLockoutCheckIntervalSeconds(),
            openIpRestrictionPolicy = getOpenIpRestrictionPolicy(),
            managementIpRestrictionPolicy = getManagementIpRestrictionPolicy(),
            mfaTokenExpirationSeconds = getMfaTokenExpirationSeconds(),
            maxRequestsPerPeriod = getMaxRequestsPerPeriod(),
            rateLimitPeriodSeconds = getRateLimitPeriodSeconds(),
            refreshTokenRotationGracePeriodSeconds = getRefreshTokenRotationGracePeriodSeconds()
        )
    }

    override fun getOpenSecuritySettings(): OpenSecuritySettings {
        return OpenSecuritySettings(
            passwordPolicy = getOpenPasswordPolicy(),
            otpConfirmation = getOtpConfirmation()
        )
    }

    override fun getRecentAuthenticationValidityInSeconds(): Int {
        return settingsService.getInt(KEY_RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS)
            ?: config.managementSecuritySettings.recentAuthenticationValiditySecondsForOpenUser
    }

    override fun getRecentAuthenticationValidityInSecondsForManagement(): Int {
        return settingsService.getInt(KEY_RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS_FOR_MANAGEMENT)
            ?: config.managementSecuritySettings.recentAuthenticationValiditySecondsForManagementUser
    }

    override fun getManagementPasswordPolicy(): ManagementPasswordPolicy {
        return settingsService.getJson(KEY_PASSWORD_POLICY) { json ->
            FoundationJson.decodeFromString<ManagementPasswordPolicyPayload>(json).toManagementPasswordPolicy()
        } ?: config.managementSecuritySettings.passwordPolicy
    }

    override fun getOpenPasswordPolicy(): OpenPasswordPolicy {
        val policy = getManagementPasswordPolicy()
        return OpenPasswordPolicy(
            minLength = policy.minLength,
            requireLetter = policy.requireLetter,
            requireUpperCase = policy.requireUpperCase,
            requireLowerCase = policy.requireLowerCase,
            requireDigit = policy.requireDigit,
            requireSpecialChar = policy.requireSpecialChar
        )
    }

    override fun getOtpConfirmation(): OtpConfirmation {
        return settingsService.getJson(KEY_OTP_CONFIRMATION) { json ->
            FoundationJson.decodeFromString<OtpConfirmationPayload>(json).toOtpConfirmation()
        } ?: config.managementSecuritySettings.otpConfirmation
    }

    override fun getAccountLockoutPolicy(): AccountLockoutPolicy {
        return settingsService.getJson(KEY_ACCOUNT_LOCKOUT_POLICY) { json ->
            FoundationJson.decodeFromString<AccountLockoutPolicyPayload>(json).toAccountLockoutPolicy()
        } ?: config.managementSecuritySettings.accountLockoutPolicy
    }

    override fun getAccountLockoutCheckIntervalSeconds(): Int {
        return settingsService.getInt(KEY_ACCOUNT_LOCKOUT_CHECK_INTERVAL_SECONDS)
            ?: config.managementSecuritySettings.accountLockoutCheckIntervalSeconds
    }

    override fun getOpenIpRestrictionPolicy(): IpRestrictionPolicy {
        val rawJson = settingsService.getString(KEY_OPEN_IP_RESTRICTION_POLICY)
            ?: return config.managementSecuritySettings.openIpRestrictionPolicy

        val cached = openIpRestrictionPolicyCache.get()
        if (cached != null && (cached.first == rawJson)) {
            return cached.second
        }

        return try {
            val parsed = FoundationJson.decodeFromString<IpRestrictionPolicyPayload>(rawJson).toIpRestrictionPolicy()
            openIpRestrictionPolicyCache.set(rawJson to parsed)
            parsed
        } catch (_: Exception) {
            config.managementSecuritySettings.openIpRestrictionPolicy
        }
    }

    override fun getManagementIpRestrictionPolicy(): IpRestrictionPolicy {
        val rawJson = settingsService.getString(KEY_MANAGEMENT_IP_RESTRICTION_POLICY)
            ?: return config.managementSecuritySettings.managementIpRestrictionPolicy

        val cached = managementIpRestrictionPolicyCache.get()
        if (cached != null && (cached.first == rawJson)) {
            return cached.second
        }

        return try {
            val parsed = FoundationJson.decodeFromString<IpRestrictionPolicyPayload>(rawJson).toIpRestrictionPolicy()
            managementIpRestrictionPolicyCache.set(rawJson to parsed)
            parsed
        } catch (_: Exception) {
            config.managementSecuritySettings.managementIpRestrictionPolicy
        }
    }

    override fun getMfaTokenExpirationSeconds(): Int {
        return settingsService.getInt(KEY_MFA_TOKEN_EXPIRATION_SECONDS)
            ?: config.managementSecuritySettings.mfaTokenExpirationSeconds
    }

    override fun getMaxRequestsPerPeriod(): Int {
        return settingsService.getInt(KEY_MAX_REQUESTS_PER_PERIOD)
            ?: config.managementSecuritySettings.maxRequestsPerPeriod
    }

    override fun getRateLimitPeriodSeconds(): Int {
        return settingsService.getInt(KEY_RATE_LIMIT_PERIOD_SECONDS)
            ?: config.managementSecuritySettings.rateLimitPeriodSeconds
    }

    override fun getRefreshTokenRotationGracePeriodSeconds(): Int {
        return settingsService.getInt(KEY_REFRESH_TOKEN_ROTATION_GRACE_PERIOD_SECONDS)
            ?: config.managementSecuritySettings.refreshTokenRotationGracePeriodSeconds
    }

    override suspend fun updateManagementSecuritySettings(
        managementSecuritySettings: ManagementSecuritySettings
    ): AppResult<Unit> {
        val settingsToUpdate = listOf(
            SystemSetting(
                key = KEY_RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS,
                value = "${managementSecuritySettings.recentAuthenticationValiditySecondsForOpenUser}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS_FOR_MANAGEMENT,
                value = "${managementSecuritySettings.recentAuthenticationValiditySecondsForManagementUser}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_PASSWORD_POLICY,
                value = FoundationJson.encodeToString(managementSecuritySettings.passwordPolicy.toManagementPasswordPolicyPayload()),
                type = SettingType.JSON
            ),
            SystemSetting(
                key = KEY_OTP_CONFIRMATION,
                value = FoundationJson.encodeToString(managementSecuritySettings.otpConfirmation.toOtpConfirmationPayload()),
                type = SettingType.JSON
            ),
            SystemSetting(
                key = KEY_ACCOUNT_LOCKOUT_POLICY,
                value = FoundationJson.encodeToString(managementSecuritySettings.accountLockoutPolicy.toAccountLockoutPolicyPayload()),
                type = SettingType.JSON
            ),
            SystemSetting(
                key = KEY_ACCOUNT_LOCKOUT_CHECK_INTERVAL_SECONDS,
                value = "${managementSecuritySettings.accountLockoutCheckIntervalSeconds}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_OPEN_IP_RESTRICTION_POLICY,
                value = FoundationJson.encodeToString(managementSecuritySettings.openIpRestrictionPolicy.toIpRestrictionPolicyPayload()),
                type = SettingType.JSON
            ),
            SystemSetting(
                key = KEY_MANAGEMENT_IP_RESTRICTION_POLICY,
                value = FoundationJson.encodeToString(managementSecuritySettings.managementIpRestrictionPolicy.toIpRestrictionPolicyPayload()),
                type = SettingType.JSON
            ),
            SystemSetting(
                key = KEY_MFA_TOKEN_EXPIRATION_SECONDS,
                value = "${managementSecuritySettings.mfaTokenExpirationSeconds}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_MAX_REQUESTS_PER_PERIOD,
                value = "${managementSecuritySettings.maxRequestsPerPeriod}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_RATE_LIMIT_PERIOD_SECONDS,
                value = "${managementSecuritySettings.rateLimitPeriodSeconds}",
                type = SettingType.INT
            ),
            SystemSetting(
                key = KEY_REFRESH_TOKEN_ROTATION_GRACE_PERIOD_SECONDS,
                value = "${managementSecuritySettings.refreshTokenRotationGracePeriodSeconds}",
                type = SettingType.INT
            )
        )
        return settingsService.updateSettings(settingsToUpdate).mapSuccess { }
    }

    override suspend fun resetManagementSecuritySettings(): AppResult<ManagementSecuritySettings> {
        val defaultSettings = config.managementSecuritySettings
        return updateManagementSecuritySettings(defaultSettings).mapSuccess { defaultSettings }
    }

    private companion object {
        const val KEY_RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS = "security.recent_authentication_validity_in_seconds"
        const val KEY_RECENT_AUTHENTICATION_VALIDITY_IN_SECONDS_FOR_MANAGEMENT = "security.recent_authentication_validity_in_seconds_for_management"
        const val KEY_PASSWORD_POLICY = "security.password_policy"
        const val KEY_OTP_CONFIRMATION = "security.otp_confirmation"
        const val KEY_ACCOUNT_LOCKOUT_POLICY = "security.account_lockout_policy"
        const val KEY_ACCOUNT_LOCKOUT_CHECK_INTERVAL_SECONDS = "security.account_lockout_check_interval_seconds"
        const val KEY_OPEN_IP_RESTRICTION_POLICY = "security.open_ip_restriction_policy"
        const val KEY_MANAGEMENT_IP_RESTRICTION_POLICY = "security.management_ip_restriction_policy"
        const val KEY_MFA_TOKEN_EXPIRATION_SECONDS = "security.mfa_token_expiration_seconds"
        const val KEY_MAX_REQUESTS_PER_PERIOD = "security.max_requests_per_period"
        const val KEY_RATE_LIMIT_PERIOD_SECONDS = "security.rate_limit_period_seconds"
        const val KEY_REFRESH_TOKEN_ROTATION_GRACE_PERIOD_SECONDS = "security.refresh_token_rotation_grace_period_seconds"
    }
}
