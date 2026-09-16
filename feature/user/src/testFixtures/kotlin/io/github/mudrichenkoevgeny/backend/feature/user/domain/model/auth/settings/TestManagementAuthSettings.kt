package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.auth.settings

import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.emailrestriction.createTestEmailRestrictionPolicy
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.AvailableAuthProviders
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.ManagementAuthSettings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.emailrestriction.EmailRestrictionPolicy

fun createTestManagementAuthSettings(
    availableAuthProviders: AvailableAuthProviders = AvailableAuthProviders(
        primary = emptyList(),
        secondary = emptyList()
    ),
    maxTotalIdentifiers: Int = 5,
    maxEmailIdentifiers: Int = 2,
    maxPhoneIdentifiers: Int = 2,
    maxIdentifiersPerExternalProvider: Int = 1,
    maxActiveSessionsForOpenUser: Int = 10,
    maxActiveSessionsForManagementUser: Int = 3,
    accessTokenExpirationSeconds: Int = 3600,
    refreshTokenExpirationSeconds: Int = 86400,
    accountDeletionGracePeriodSeconds: Int = 2592000,
    accountDeletionCheckIntervalSeconds: Int = 60,
    isRegistrationEnabled: Boolean = true,
    openEmailRestrictionPolicy: EmailRestrictionPolicy = createTestEmailRestrictionPolicy(),
    managementEmailRestrictionPolicy: EmailRestrictionPolicy = createTestEmailRestrictionPolicy()
): ManagementAuthSettings = ManagementAuthSettings(
    availableAuthProviders = availableAuthProviders,
    maxTotalIdentifiers = maxTotalIdentifiers,
    maxEmailIdentifiers = maxEmailIdentifiers,
    maxPhoneIdentifiers = maxPhoneIdentifiers,
    maxIdentifiersPerExternalProvider = maxIdentifiersPerExternalProvider,
    maxActiveSessionsForOpenUser = maxActiveSessionsForOpenUser,
    maxActiveSessionsForManagementUser = maxActiveSessionsForManagementUser,
    accessTokenExpirationSeconds = accessTokenExpirationSeconds,
    refreshTokenExpirationSeconds = refreshTokenExpirationSeconds,
    accountDeletionGracePeriodSeconds = accountDeletionGracePeriodSeconds,
    accountDeletionCheckIntervalSeconds = accountDeletionCheckIntervalSeconds,
    isRegistrationEnabled = isRegistrationEnabled,
    openEmailRestrictionPolicy = openEmailRestrictionPolicy,
    managementEmailRestrictionPolicy = managementEmailRestrictionPolicy
)