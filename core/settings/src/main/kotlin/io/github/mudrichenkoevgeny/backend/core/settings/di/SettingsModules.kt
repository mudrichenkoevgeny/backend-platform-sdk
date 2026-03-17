package io.github.mudrichenkoevgeny.backend.core.settings.di

import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.settings.di.module.SettingsConfigModule
import io.github.mudrichenkoevgeny.backend.core.settings.di.module.SettingsManagersModule
import io.github.mudrichenkoevgeny.backend.core.settings.di.module.GlobalSettingsProviderModule
import io.github.mudrichenkoevgeny.backend.core.settings.di.module.SettingsRepositoriesModule
import io.github.mudrichenkoevgeny.backend.core.settings.di.module.SettingsServicesModule
import io.github.mudrichenkoevgeny.backend.core.settings.di.module.SettingsWebSocketModule

/**
 * Dagger module aggregator for the settings feature.
 *
 * Includes configuration parsing, repositories/managers/services, global settings provider and
 * WebSocket message handlers related to settings.
 */
@Module(
    includes = [
        SettingsConfigModule::class,
        SettingsRepositoriesModule::class,
        SettingsManagersModule::class,
        SettingsServicesModule::class,
        GlobalSettingsProviderModule::class,
        SettingsWebSocketModule::class
    ]
)
interface SettingsModules