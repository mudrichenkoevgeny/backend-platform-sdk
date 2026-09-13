package io.github.mudrichenkoevgeny.backend.core.security.config.model

import io.github.mudrichenkoevgeny.backend.core.security.domain.model.otpconfirmation.createTestOtpConfirmation
import io.github.mudrichenkoevgeny.backend.core.security.domain.model.passwordpolicy.createTestManagementPasswordPolicy
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
    mfaTokenExpirationSeconds: Int = 120,
    maxRequestsPerPeriod: Int = 100,
    rateLimitPeriodSeconds: Int = 60
) = SecurityConfig(
    authRealm = authRealm,
    totpEncryptionSecret = totpEncryptionSecret,
    recentAuthenticationValidityInSeconds = recentAuthenticationValidityInSeconds,
    recentAuthenticationValidityInSecondsForManagement = recentAuthenticationValidityInSecondsForManagement,
    passwordPolicy = passwordPolicy,
    otpConfirmation = otpConfirmation,
    mfaTokenExpirationSeconds = mfaTokenExpirationSeconds,
    maxRequestsPerPeriod = maxRequestsPerPeriod,
    rateLimitPeriodSeconds = rateLimitPeriodSeconds
)

fun createTestManagementSecuritySettings(
    recentAuthenticationValiditySecondsForOpenUser: Int = 30,
    recentAuthenticationValiditySecondsForManagementUser: Int = 60,
    passwordPolicy: ManagementPasswordPolicy = createTestManagementPasswordPolicy(),
    otpConfirmation: OtpConfirmation = createTestOtpConfirmation(),
    mfaTokenExpirationSeconds: Int = 120,
    maxRequestsPerPeriod: Int = 100,
    rateLimitPeriodSeconds: Int = 60
) = ManagementSecuritySettings(
    recentAuthenticationValiditySecondsForOpenUser = recentAuthenticationValiditySecondsForOpenUser,
    recentAuthenticationValiditySecondsForManagementUser = recentAuthenticationValiditySecondsForManagementUser,
    passwordPolicy = passwordPolicy,
    otpConfirmation = otpConfirmation,
    accountLockoutPolicy = SecurityConfig.DEFAULT_ACCOUNT_LOCKOUT_POLICY,
    openIpRestrictionPolicy = SecurityConfig.DEFAULT_IP_RESTRICTION_POLICY,
    managementIpRestrictionPolicy = SecurityConfig.DEFAULT_IP_RESTRICTION_POLICY,
    mfaTokenExpirationSeconds = mfaTokenExpirationSeconds,
    maxRequestsPerPeriod = maxRequestsPerPeriod,
    rateLimitPeriodSeconds = rateLimitPeriodSeconds
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
