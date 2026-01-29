package io.github.mudrichenkoevgeny.backend.core.events.di.module

import io.github.mudrichenkoevgeny.backend.core.events.subscriber.EventSubscriber
import io.github.mudrichenkoevgeny.backend.core.events.subscriber.EventSubscriberImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
interface EventSubscriberModule {

    @Binds
    @Singleton
    fun bindEventSubscriber(eventSubscriberImpl: EventSubscriberImpl): EventSubscriber
}