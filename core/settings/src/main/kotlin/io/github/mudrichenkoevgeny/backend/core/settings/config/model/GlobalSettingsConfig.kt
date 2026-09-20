package io.github.mudrichenkoevgeny.backend.core.settings.config.model

import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.globalsettings.ManagementGlobalSettings

/**
 * Static settings configuration loaded from the environment at application startup.
 *
 * This config acts as a seed for DB-backed settings (see `global` settings provider) and is typically
 * created by `GlobalSettingsConfigFactory`.
 *
 * @param managementGlobalSettings default management global settings configuration
 */
data class GlobalSettingsConfig(
    val managementGlobalSettings: ManagementGlobalSettings
)
