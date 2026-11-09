package io.github.mudrichenkoevgeny.backend.core.events.di

import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.events.di.module.EventPublisherModule
import io.github.mudrichenkoevgeny.backend.core.events.di.module.EventSubscriberModule
import io.github.mudrichenkoevgeny.backend.core.events.di.module.EventsConfigModule

/**
 * Aggregate Dagger module for the events feature. Includes [EventsConfigModule], [EventPublisherModule], and [EventSubscriberModule].
 */
@Module(
    includes = [EventsConfigModule::class, EventPublisherModule::class, EventSubscriberModule::class]
)
public interface EventsModules
