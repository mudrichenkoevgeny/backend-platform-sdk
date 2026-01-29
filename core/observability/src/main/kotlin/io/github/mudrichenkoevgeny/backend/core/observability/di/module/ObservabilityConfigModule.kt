package io.github.mudrichenkoevgeny.backend.core.observability.di.module

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.observability.config.factory.ObservabilityConfigFactory
import io.github.mudrichenkoevgeny.backend.core.observability.config.factory.ObservabilityConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.observability.config.model.ObservabilityConfig
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class ObservabilityConfigModule {

    @Provides
    @Singleton
    fun provideObservabilityConfigFactory(
        envReader: EnvReader
    ): ObservabilityConfigFactory {
        return ObservabilityConfigFactoryImpl(
            envReader = envReader
        )
    }

    @Provides
    @Singleton
    fun provideObservabilityConfig(
        observabilityConfigFactory: ObservabilityConfigFactory
    ): ObservabilityConfig {
        return observabilityConfigFactory.create()
    }
}