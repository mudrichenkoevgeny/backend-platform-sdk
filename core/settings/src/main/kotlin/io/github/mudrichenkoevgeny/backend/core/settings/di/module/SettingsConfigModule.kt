package io.github.mudrichenkoevgeny.backend.core.settings.di.module

import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.settings.config.factory.SettingsConfigFactory
import io.github.mudrichenkoevgeny.backend.core.settings.config.factory.SettingsConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.settings.config.model.SettingsConfig
import javax.inject.Singleton

/**
 * Dagger module that wires settings configuration.
 *
 * Provides:
 * - [SettingsConfigFactory] backed by [SettingsConfigFactoryImpl]
 * - [SettingsConfig] created from the factory at injection time
 */
@Module
class SettingsConfigModule {

    @Provides
    @Singleton
    fun provideSettingsConfigFactory(
        envReader: EnvReader
    ): SettingsConfigFactory {
        return SettingsConfigFactoryImpl(
            envReader = envReader
        )
    }

    @Provides
    @Singleton
    fun provideSettingsConfig(
        settingsConfigFactory: SettingsConfigFactory
    ): SettingsConfig {
        return settingsConfigFactory.create()
    }
}