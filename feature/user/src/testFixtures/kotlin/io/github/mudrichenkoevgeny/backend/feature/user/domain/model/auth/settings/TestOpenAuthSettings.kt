package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.auth.settings

import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.AvailableAuthProviders
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.auth.settings.OpenAuthSettings

fun createTestOpenAuthSettings(
    availableAuthProviders: AvailableAuthProviders = AvailableAuthProviders(
        primary = emptyList(),
        secondary = emptyList()
    ),
    maxTotalIdentifiers: Int = 5,
    maxEmailIdentifiers: Int = 2,
    maxPhoneIdentifiers: Int = 2,
    maxIdentifiersPerExternalProvider: Int = 1,
    isRegistrationEnabled: Boolean = true
): OpenAuthSettings = OpenAuthSettings(
    availableAuthProviders = availableAuthProviders,
    maxTotalIdentifiers = maxTotalIdentifiers,
    maxEmailIdentifiers = maxEmailIdentifiers,
    maxPhoneIdentifiers = maxPhoneIdentifiers,
    maxIdentifiersPerExternalProvider = maxIdentifiersPerExternalProvider,
    isRegistrationEnabled = isRegistrationEnabled
)
