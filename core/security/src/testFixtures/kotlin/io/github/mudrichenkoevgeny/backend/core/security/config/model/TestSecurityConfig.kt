package io.github.mudrichenkoevgeny.backend.core.security.config.model

import io.github.mudrichenkoevgeny.backend.core.security.domain.model.otpconfirmation.createTestOtpConfirmation
import io.github.mudrichenkoevgeny.backend.core.security.domain.model.passwordpolicy.createTestManagementPasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.iprestriction.IpRestrictionPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.otpconfirmation.OtpConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.ManagementPasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.OpenPasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.ManagementSecuritySettings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.securitysettings.OpenSecuritySettings

fun createTestSecurityConfig(
    authRealm: String = "test-realm",
    totpEncryptionSecret: String = "test-secret",
    recentAuthenticationValidityInSeconds: Int = 30,
    recentAuthenticationValidityInSecondsForManagement: Int = 60,
    passwordPolicy: ManagementPasswordPolicy = createTestManagementPasswordPolicy(),
    otpConfirmation: OtpConfirmation = createTestOtpConfirmation(),
    accountLockoutPolicy: AccountLockoutPolicy = SecurityConfig.DEFAULT_ACCOUNT_LOCKOUT_POLICY,
    accountLockoutCheckIntervalSeconds: Int = SecurityConfig.DEFAULT_ACCOUNT_LOCKOUT_CHECK_INTERVAL_SECONDS,
    openIpRestrictionPolicy: IpRestrictionPolicy = SecurityConfig.DEFAULT_IP_RESTRICTION_POLICY,
    managementIpRestrictionPolicy: IpRestrictionPolicy = SecurityConfig.DEFAULT_IP_RESTRICTION_POLICY,
    mfaTokenExpirationSeconds: Int = 120,
    maxRequestsPerPeriod: Int = SecurityConfig.DEFAULT_MAX_REQUESTS_PER_PERIOD,
    rateLimitPeriodSeconds: Int = SecurityConfig.DEFAULT_RATE_LIMIT_PERIOD_SECONDS,
    refreshTokenRotationGracePeriodSeconds: Int = SecurityConfig.DEFAULT_REFRESH_TOKEN_ROTATION_GRACE_PERIOD_SECONDS
) = SecurityConfig(
    authRealm = authRealm,
    totpEncryptionSecret = totpEncryptionSecret,
    recentAuthenticationValidityInSeconds = recentAuthenticationValidityInSeconds,
    recentAuthenticationValidityInSecondsForManagement = recentAuthenticationValidityInSecondsForManagement,
    passwordPolicy = passwordPolicy,
    otpConfirmation = otpConfirmation,
    accountLockoutPolicy = accountLockoutPolicy,
    accountLockoutCheckIntervalSeconds = accountLockoutCheckIntervalSeconds,
    openIpRestrictionPolicy = openIpRestrictionPolicy,
    managementIpRestrictionPolicy = managementIpRestrictionPolicy,
    mfaTokenExpirationSeconds = mfaTokenExpirationSeconds,
    maxRequestsPerPeriod = maxRequestsPerPeriod,
    rateLimitPeriodSeconds = rateLimitPeriodSeconds,
    refreshTokenRotationGracePeriodSeconds = refreshTokenRotationGracePeriodSeconds
)

fun createTestManagementSecuritySettings(
    recentAuthenticationValiditySecondsForOpenUser: Int = 30,
    recentAuthenticationValiditySecondsForManagementUser: Int = 60,
    passwordPolicy: ManagementPasswordPolicy = createTestManagementPasswordPolicy(),
    otpConfirmation: OtpConfirmation = createTestOtpConfirmation(),
    accountLockoutPolicy: AccountLockoutPolicy = SecurityConfig.DEFAULT_ACCOUNT_LOCKOUT_POLICY,
    accountLockoutCheckIntervalSeconds: Int = SecurityConfig.DEFAULT_ACCOUNT_LOCKOUT_CHECK_INTERVAL_SECONDS,
    openIpRestrictionPolicy: IpRestrictionPolicy = SecurityConfig.DEFAULT_IP_RESTRICTION_POLICY,
    managementIpRestrictionPolicy: IpRestrictionPolicy = SecurityConfig.DEFAULT_IP_RESTRICTION_POLICY,
    mfaTokenExpirationSeconds: Int = 120,
    maxRequestsPerPeriod: Int = SecurityConfig.DEFAULT_MAX_REQUESTS_PER_PERIOD,
    rateLimitPeriodSeconds: Int = SecurityConfig.DEFAULT_RATE_LIMIT_PERIOD_SECONDS,
    refreshTokenRotationGracePeriodSeconds: Int = SecurityConfig.DEFAULT_REFRESH_TOKEN_ROTATION_GRACE_PERIOD_SECONDS
) = ManagementSecuritySettings(
    recentAuthenticationValiditySecondsForOpenUser = recentAuthenticationValiditySecondsForOpenUser,
    recentAuthenticationValiditySecondsForManagementUser = recentAuthenticationValiditySecondsForManagementUser,
    passwordPolicy = passwordPolicy,
    otpConfirmation = otpConfirmation,
    accountLockoutPolicy = accountLockoutPolicy,
    accountLockoutCheckIntervalSeconds = accountLockoutCheckIntervalSeconds,
    openIpRestrictionPolicy = openIpRestrictionPolicy,
    managementIpRestrictionPolicy = managementIpRestrictionPolicy,
    mfaTokenExpirationSeconds = mfaTokenExpirationSeconds,
    maxRequestsPerPeriod = maxRequestsPerPeriod,
    rateLimitPeriodSeconds = rateLimitPeriodSeconds,
    refreshTokenRotationGracePeriodSeconds = refreshTokenRotationGracePeriodSeconds
)

fun createTestOpenSecuritySettings(
    passwordPolicy: OpenPasswordPolicy = OpenPasswordPolicy(
        minLength = 8,
        requireLetter = true,
        requireUpperCase = true,
        requireLowerCase = true,
        requireDigit = true,
        requireSpecialChar = false
    ),
    otpConfirmation: OtpConfirmation = createTestOtpConfirmation()
) = OpenSecuritySettings(
    passwordPolicy = passwordPolicy,
    otpConfirmation = otpConfirmation
)