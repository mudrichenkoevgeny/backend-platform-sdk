package io.github.mudrichenkoevgeny.backend.core.events.di.module

import io.github.mudrichenkoevgeny.backend.core.events.publisher.EventPublisher
import io.github.mudrichenkoevgeny.backend.core.events.publisher.EventPublisherImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
interface EventPublisherModule {

    @Binds
    @Singleton
    fun bindEventPublisher(eventPublisherImpl: EventPublisherImpl): EventPublisher
}