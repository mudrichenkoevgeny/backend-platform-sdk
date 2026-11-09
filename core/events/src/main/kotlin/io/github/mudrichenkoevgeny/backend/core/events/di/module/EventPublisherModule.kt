package io.github.mudrichenkoevgeny.backend.core.events.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.events.publisher.EventPublisher
import io.github.mudrichenkoevgeny.backend.core.events.publisher.EventPublisherImpl
import javax.inject.Singleton

/**
 * Dagger module that binds [EventPublisher] to [EventPublisherImpl].
 */
@Module
public interface EventPublisherModule {
    @Binds
    @Singleton
    public fun bindEventPublisher(eventPublisherImpl: EventPublisherImpl): EventPublisher
}
