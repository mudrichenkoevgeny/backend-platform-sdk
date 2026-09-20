package io.github.mudrichenkoevgeny.backend.feature.user.provider.authsettings

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.AvailableAuthProviders
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.OpenAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.emailrestriction.EmailRestrictionPolicy

class TestAuthSettingsProvider : AuthSettingsProvider {

    var initializeCalled: Boolean = false
    var initializeResult: AppResult<Unit> = AppResult.Success(Unit)

    var updateManagementAuthSettingsCalled: Boolean = false
    var lastUpdatedManagementAuthSettings: ManagementAuthSettings? = null
    var updateManagementAuthSettingsResult: AppResult<Unit> = AppResult.Success(Unit)

    var resetManagementAuthSettingsCalled: Boolean = false
    var resetManagementAuthSettingsResult: AppResult<ManagementAuthSettings>? = null

    var currentManagementAuthSettings: ManagementAuthSettings? = null
    var currentOpenAuthSettings: OpenAuthSettings? = null
    var currentAvailableAuthProviders: AvailableAuthProviders? = null
    var currentOpenEmailRestrictionPolicy: EmailRestrictionPolicy = EmailRestrictionPolicy(
        isBlacklistEnabled = false,
        blacklist = emptyList(),
        isWhitelistEnabled = false,
        whitelist = emptyList()
    )
    var currentManagementEmailRestrictionPolicy: EmailRestrictionPolicy = EmailRestrictionPolicy(
        isBlacklistEnabled = false,
        blacklist = emptyList(),
        isWhitelistEnabled = false,
        whitelist = emptyList()
    )

    var configuredMaxTotalIdentifiers: Int = 5
    var configuredMaxEmailIdentifiers: Int = 2
    var configuredMaxPhoneIdentifiers: Int = 1
    var configuredMaxIdentifiersPerExternalProvider: Int = 1
    var configuredMaxActiveSessionsForOpenUser: Int = 5
    var configuredMaxActiveSessionsForManagementUser: Int = 3
    var configuredAccessTokenExpirationSeconds: Int = 3600
    var configuredRefreshTokenExpirationSeconds: Int = 2592000
    var configuredAccountDeletionGracePeriodSeconds: Int = 2592000
    var configuredAccountDeletionCheckIntervalSeconds: Int = 60
    var configuredIsRegistrationEnabled: Boolean = true

    override suspend fun initialize(): AppResult<Unit> {
        initializeCalled = true
        return initializeResult
    }

    override fun getManagementAuthSettings(): ManagementAuthSettings {
        return currentManagementAuthSettings ?: error("currentManagementAuthSettings is not set")
    }

    override fun getOpenAuthSettings(): OpenAuthSettings {
        return currentOpenAuthSettings ?: error("currentOpenAuthSettings is not set")
    }

    override fun getAvailableAuthProviders(): AvailableAuthProviders {
        return currentAvailableAuthProviders ?: error("currentAvailableAuthProviders is not set")
    }

    override fun getMaxTotalIdentifiers(): Int = configuredMaxTotalIdentifiers
    override fun getMaxEmailIdentifiers(): Int = configuredMaxEmailIdentifiers
    override fun getMaxPhoneIdentifiers(): Int = configuredMaxPhoneIdentifiers
    override fun getMaxIdentifiersPerExternalProvider(): Int = configuredMaxIdentifiersPerExternalProvider
    override fun getMaxActiveSessionsForOpenUser(): Int = configuredMaxActiveSessionsForOpenUser
    override fun getMaxActiveSessionsForManagementUser(): Int = configuredMaxActiveSessionsForManagementUser
    override fun getAccessTokenExpirationSeconds(): Int = configuredAccessTokenExpirationSeconds
    override fun getRefreshTokenExpirationSeconds(): Int = configuredRefreshTokenExpirationSeconds
    override fun getAccountDeletionGracePeriodSeconds(): Int = configuredAccountDeletionGracePeriodSeconds
    override fun getAccountDeletionCheckIntervalSeconds(): Int = configuredAccountDeletionCheckIntervalSeconds
    override fun getIsRegistrationEnabled(): Boolean = configuredIsRegistrationEnabled
    override fun getOpenEmailRestrictionPolicy(): EmailRestrictionPolicy = currentOpenEmailRestrictionPolicy
    override fun getManagementEmailRestrictionPolicy(): EmailRestrictionPolicy = currentManagementEmailRestrictionPolicy

    override suspend fun updateManagementAuthSettings(
        managementAuthSettings: ManagementAuthSettings
    ): AppResult<Unit> {
        updateManagementAuthSettingsCalled = true
        lastUpdatedManagementAuthSettings = managementAuthSettings
        return updateManagementAuthSettingsResult
    }

    override suspend fun resetManagementAuthSettings(): AppResult<ManagementAuthSettings> {
        resetManagementAuthSettingsCalled = true
        return resetManagementAuthSettingsResult
            ?: currentManagementAuthSettings?.let { AppResult.Success(it) }
            ?: error("resetManagementAuthSettingsResult or currentManagementAuthSettings must be set")
    }
}