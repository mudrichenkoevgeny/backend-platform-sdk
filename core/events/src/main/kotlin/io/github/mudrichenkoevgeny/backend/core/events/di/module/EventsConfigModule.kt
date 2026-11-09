package io.github.mudrichenkoevgeny.backend.core.events.di.module

import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.events.config.factory.EventsConfigFactory
import io.github.mudrichenkoevgeny.backend.core.events.config.factory.EventsConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.events.config.model.EventsConfig
import javax.inject.Singleton

/**
 * Dagger module that provides [EventsConfigFactory] (bound to [EventsConfigFactoryImpl]) and [EventsConfig] (from the factory).
 */
@Module
public class EventsConfigModule {
    @Provides
    @Singleton
    public fun provideEventsConfigFactory(envReader: EnvReader): EventsConfigFactory = EventsConfigFactoryImpl(
        envReader = envReader
    )

    @Provides
    @Singleton
    public fun provideEventsConfig(eventsConfigFactory: EventsConfigFactory): EventsConfig =
        eventsConfigFactory.create()
}
