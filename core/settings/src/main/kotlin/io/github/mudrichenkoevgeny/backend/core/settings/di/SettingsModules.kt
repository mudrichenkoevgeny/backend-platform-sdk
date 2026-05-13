package io.github.mudrichenkoevgeny.backend.core.settings.di

import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.settings.di.module.SettingsConfigModule
import io.github.mudrichenkoevgeny.backend.core.settings.di.module.SettingsManagersModule
import io.github.mudrichenkoevgeny.backend.core.settings.di.module.GlobalSettingsProviderModule
import io.github.mudrichenkoevgeny.backend.core.settings.di.module.SettingsRepositoriesModule
import io.github.mudrichenkoevgeny.backend.core.settings.di.module.SettingsServicesModule
/**
 * Dagger module aggregator for the settings feature.
 *
 * Includes configuration parsing, repositories/managers/services, and global settings provider.
 * WebSocket handlers for settings are registered from the `feature/settingsapi` module (`SettingsApiModules`).
 */
@Module(
    includes = [
        SettingsConfigModule::class,
        SettingsRepositoriesModule::class,
        SettingsManagersModule::class,
        SettingsServicesModule::class,
        GlobalSettingsProviderModule::class
    ]
)
interface SettingsModules