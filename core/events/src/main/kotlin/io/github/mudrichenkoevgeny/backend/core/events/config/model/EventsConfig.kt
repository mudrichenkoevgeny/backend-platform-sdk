package io.github.mudrichenkoevgeny.backend.core.events.config.model

import io.github.mudrichenkoevgeny.backend.core.events.model.EventsType
import io.github.mudrichenkoevgeny.backend.core.events.publisher.EventPublisher
import io.github.mudrichenkoevgeny.backend.core.events.subscriber.EventSubscriber

/**
 * Configuration for the events module: transport type and Kafka parameters.
 *
 * @param eventsType transport type ([EventsType.KAFKA] or [EventsType.IN_MEMORY]); determines which [EventPublisher]/[EventSubscriber] implementation is used.
 * @param kafkaBootstrapServers Kafka bootstrap servers string (e.g. `localhost:9092`).
 * @param kafkaGroupId consumer group id for the subscriber.
 * @param kafkaClientId client id for producer/consumer.
 */
data class EventsConfig(
    val eventsType: EventsType,
    val kafkaBootstrapServers: String,
    val kafkaGroupId: String,
    val kafkaClientId: String
)