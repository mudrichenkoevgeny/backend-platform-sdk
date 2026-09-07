package io.github.mudrichenkoevgeny.backend.core.settings.di.module

import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.settings.config.factory.GlobalSettingsConfigFactory
import io.github.mudrichenkoevgeny.backend.core.settings.config.factory.GlobalSettingsConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.settings.config.model.GlobalSettingsConfig
import javax.inject.Singleton

/**
 * Dagger module that wires settings configuration.
 *
 * Provides:
 * - [GlobalSettingsConfigFactory] backed by [GlobalSettingsConfigFactoryImpl]
 * - [GlobalSettingsConfig] created from the factory at injection time
 */
@Module
class SettingsConfigModule {

    @Provides
    @Singleton
    fun provideSettingsConfigFactory(
        envReader: EnvReader
    ): GlobalSettingsConfigFactory {
        return GlobalSettingsConfigFactoryImpl(
            envReader = envReader
        )
    }

    @Provides
    @Singleton
    fun provideSettingsConfig(
        globalSettingsConfigFactory: GlobalSettingsConfigFactory
    ): GlobalSettingsConfig {
        return globalSettingsConfigFactory.create()
    }
}