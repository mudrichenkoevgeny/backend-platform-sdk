package io.github.mudrichenkoevgeny.backend.feature.settings.api.di

import dagger.Module
import io.github.mudrichenkoevgeny.backend.feature.settings.api.di.module.SettingsWebSocketModule

/**
 * Dagger aggregation module for the `feature/settings-api` package.
 *
 * Includes:
 * - [SettingsWebSocketModule] (WebSocket handlers contributed into the global handler set)
 */
@Module(
    includes = [
        SettingsWebSocketModule::class
    ]
)
interface SettingsApiModules
