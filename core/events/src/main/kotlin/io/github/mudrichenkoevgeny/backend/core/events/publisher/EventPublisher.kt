package io.github.mudrichenkoevgeny.backend.core.events.publisher

import io.github.mudrichenkoevgeny.backend.core.events.event.AppEvent
import io.github.mudrichenkoevgeny.backend.core.events.model.EventsType
import kotlinx.serialization.KSerializer

/**
 * Contract for publishing [AppEvent] instances to a given topic (e.g. Kafka).
 *
 * The implementation is chosen by [EventsType] from configuration; the typical implementation is [EventPublisherImpl] (Kafka).
 * Events are serialized to JSON using the provided [KSerializer].
 */
interface EventPublisher {

    /**
     * Publishes an event to the specified topic.
     *
     * @param topic the topic name (e.g. Kafka topic).
     * @param event the event instance implementing [AppEvent].
     * @param serializer serializer for type [T]; used for JSON serialization of the message body.
     * @param metadata optional metadata (e.g. message headers); defaults to an empty map.
     */
    suspend fun <T : AppEvent> publish(
        topic: String,
        event: T,
        serializer: KSerializer<T>,
        metadata: Map<String, String> = emptyMap()
    )
}