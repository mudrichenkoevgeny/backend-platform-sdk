package io.github.mudrichenkoevgeny.backend.core.security.settings.provider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.config.model.createTestManagementSecuritySettings
import io.github.mudrichenkoevgeny.backend.core.security.config.model.createTestOpenSecuritySettings
import io.github.mudrichenkoevgeny.backend.core.security.domain.model.iprestriction.createTestIpRestrictionPolicy
import io.github.mudrichenkoevgeny.backend.core.security.domain.model.otpconfirmation.createTestOtpConfirmation
import io.github.mudrichenkoevgeny.backend.core.security.domain.model.passwordpolicy.createTestManagementPasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.iprestriction.IpRestrictionPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.ManagementPasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.OpenPasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.ManagementSecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.OpenSecuritySettings

class RecordingSecuritySettingsProvider : SecuritySettingsProvider {
    var initializeCalled: Boolean = false

    override suspend fun initialize(): AppResult<Unit> {
        initializeCalled = true
        return AppResult.Success(Unit)
    }

    override fun getManagementSecuritySettings(): ManagementSecuritySettings = createTestManagementSecuritySettings()

    override fun getOpenSecuritySettings(): OpenSecuritySettings = createTestOpenSecuritySettings()

    override fun getRecentAuthenticationValidityInSeconds(): Int = 30

    override fun getRecentAuthenticationValidityInSecondsForManagement(): Int = 60

    override fun getManagementPasswordPolicy(): ManagementPasswordPolicy = createTestManagementPasswordPolicy()

    override fun getOpenPasswordPolicy(): OpenPasswordPolicy = getOpenSecuritySettings().passwordPolicy

    override fun getOtpConfirmation(): OtpConfirmation = createTestOtpConfirmation()

    override fun getAccountLockoutPolicy(): AccountLockoutPolicy = error("Not used")

    var currentOpenIpRestrictionPolicy: IpRestrictionPolicy = createTestIpRestrictionPolicy()

    var currentManagementIpRestrictionPolicy: IpRestrictionPolicy = createTestIpRestrictionPolicy()

    override fun getOpenIpRestrictionPolicy(): IpRestrictionPolicy = currentOpenIpRestrictionPolicy

    override fun getManagementIpRestrictionPolicy(): IpRestrictionPolicy = currentManagementIpRestrictionPolicy

    override fun getMfaTokenExpirationSeconds(): Int = 120

    override fun getMaxRequestsPerPeriod(): Int = 100

    override fun getRateLimitPeriodSeconds(): Int = 60

    override suspend fun updateManagementSecuritySettings(
        managementSecuritySettings: ManagementSecuritySettings
    ): AppResult<Unit> = AppResult.Success(Unit)
}