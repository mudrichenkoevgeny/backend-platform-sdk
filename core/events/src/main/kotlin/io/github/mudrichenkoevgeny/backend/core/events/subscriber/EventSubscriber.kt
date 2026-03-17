package io.github.mudrichenkoevgeny.backend.core.events.subscriber

import io.github.mudrichenkoevgeny.backend.core.events.event.AppEvent
import io.github.mudrichenkoevgeny.backend.core.events.model.EventsType

/**
 * Contract for subscribing to [AppEvent] instances from a given topic (e.g. Kafka).
 *
 * The implementation is chosen by [EventsType] from configuration. The subscriber deserializes messages to type [T] and
 * invokes the handler for each record; metadata (headers) is passed as the second argument.
 */
interface EventSubscriber {

    /**
     * Registers a subscription to the topic and processes incoming events with the given handler.
     *
     * @param topic the topic name to subscribe to.
     * @param type the event class [T] used to deserialize the message body.
     * @param handler coroutine invoked for each event; receives the deserialized event and metadata (headers).
     */
    fun <T : AppEvent> subscribe(
        topic: String,
        type: Class<T>,
        handler: suspend (event: T, metadata: Map<String, String>) -> Unit
    )
}