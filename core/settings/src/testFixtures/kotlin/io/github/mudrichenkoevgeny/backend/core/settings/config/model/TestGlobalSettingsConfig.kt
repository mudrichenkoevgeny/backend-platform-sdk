package io.github.mudrichenkoevgeny.backend.core.settings.config.model

import io.github.mudrichenkoevgeny.backend.core.settings.domain.model.globalsettings.createTestManagementGlobalSettings
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.ManagementGlobalSettings

fun createTestGlobalSettingsConfig(
    managementGlobalSettings: ManagementGlobalSettings = createTestManagementGlobalSettings()
) = GlobalSettingsConfig(
    managementGlobalSettings = managementGlobalSettings
)
