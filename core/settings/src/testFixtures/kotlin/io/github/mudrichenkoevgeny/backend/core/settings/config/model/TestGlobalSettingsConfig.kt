package io.github.mudrichenkoevgeny.backend.core.settings.config.model

import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.ManagementGlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.testing.core.settings.domain.model.globalsettings.createTestManagementGlobalSettings

fun createTestGlobalSettingsConfig(
    managementGlobalSettings: ManagementGlobalSettings = createTestManagementGlobalSettings()
) = GlobalSettingsConfig(
    managementGlobalSettings = managementGlobalSettings
)
