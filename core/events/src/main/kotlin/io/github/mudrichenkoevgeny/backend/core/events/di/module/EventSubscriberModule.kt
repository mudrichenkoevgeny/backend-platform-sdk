package io.github.mudrichenkoevgeny.backend.core.events.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.events.subscriber.EventSubscriber
import io.github.mudrichenkoevgeny.backend.core.events.subscriber.EventSubscriberImpl
import javax.inject.Singleton

/**
 * Dagger module that binds [EventSubscriber] to [EventSubscriberImpl].
 */
@Module
public interface EventSubscriberModule {
    @Binds
    @Singleton
    public fun bindEventSubscriber(eventSubscriberImpl: EventSubscriberImpl): EventSubscriber
}
