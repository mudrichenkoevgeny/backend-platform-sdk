package io.github.mudrichenkoevgeny.backend.core.events.event

import io.github.mudrichenkoevgeny.backend.core.events.publisher.EventPublisher
import io.github.mudrichenkoevgeny.backend.core.events.subscriber.EventSubscriber
import kotlinx.serialization.KSerializer

/**
 * Marker interface for application events published and consumed via [EventPublisher] and [EventSubscriber].
 *
 * All domain events sent over the event bus (e.g. Kafka or in-memory) must implement this interface and be serializable
 * (Kotlin Serialization). The event type is specified at publish and subscribe time via [KSerializer].
 */
interface AppEvent