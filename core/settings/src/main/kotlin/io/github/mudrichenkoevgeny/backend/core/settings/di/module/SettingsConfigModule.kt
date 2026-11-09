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
public class SettingsConfigModule {
    @Provides
    @Singleton
    public fun provideSettingsConfigFactory(envReader: EnvReader): GlobalSettingsConfigFactory =
        GlobalSettingsConfigFactoryImpl(
            envReader = envReader
        )

    @Provides
    @Singleton
    public fun provideSettingsConfig(globalSettingsConfigFactory: GlobalSettingsConfigFactory): GlobalSettingsConfig =
        globalSettingsConfigFactory.create()
}
