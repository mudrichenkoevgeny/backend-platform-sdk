package io.github.mudrichenkoevgeny.backend.core.observability.di.module

import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.observability.config.factory.ObservabilityConfigFactory
import io.github.mudrichenkoevgeny.backend.core.observability.config.factory.ObservabilityConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.observability.config.model.ObservabilityConfig
import javax.inject.Singleton

/**
 * Dagger module for observability configuration.
 *
 * Provides [ObservabilityConfigFactory] (bound to [ObservabilityConfigFactoryImpl] with [EnvReader])
 * and [ObservabilityConfig] from the factory.
 */
@Module
public class ObservabilityConfigModule {
    @Provides
    @Singleton
    public fun provideObservabilityConfigFactory(envReader: EnvReader): ObservabilityConfigFactory =
        ObservabilityConfigFactoryImpl(
            envReader = envReader
        )

    @Provides
    @Singleton
    public fun provideObservabilityConfig(observabilityConfigFactory: ObservabilityConfigFactory): ObservabilityConfig =
        observabilityConfigFactory.create()
}
