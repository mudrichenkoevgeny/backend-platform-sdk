package io.github.mudrichenkoevgeny.backend.feature.settingsapi.di

import dagger.Module
import io.github.mudrichenkoevgeny.backend.feature.settingsapi.di.module.SettingsWebSocketModule

/**
 * Dagger aggregation module for the `feature/settingsapi` package.
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
