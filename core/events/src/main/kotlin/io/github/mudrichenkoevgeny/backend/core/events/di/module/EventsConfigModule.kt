package io.github.mudrichenkoevgeny.backend.core.events.di.module

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.events.config.factory.EventsConfigFactory
import io.github.mudrichenkoevgeny.backend.core.events.config.factory.EventsConfigFactoryImpl
import io.github.mudrichenkoevgeny.backend.core.events.config.model.EventsConfig
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class EventsConfigModule {

    @Provides
    @Singleton
    fun provideEventsConfigFactory(
        envReader: EnvReader
    ): EventsConfigFactory {
        return EventsConfigFactoryImpl(
            envReader = envReader
        )
    }

    @Provides
    @Singleton
    fun provideEventsConfig(
        eventsConfigFactory: EventsConfigFactory
    ): EventsConfig {
        return eventsConfigFactory.create()
    }
}