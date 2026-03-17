package io.github.mudrichenkoevgeny.backend.core.events.config.envkeys

import io.github.mudrichenkoevgeny.backend.core.events.config.model.EventsConfig
import io.github.mudrichenkoevgeny.backend.core.events.config.factory.EventsConfigFactory

/**
 * Environment variable keys used to build [EventsConfig]. Values are read by [EventsConfigFactory] implementations.
 */
object EventsEnvKeys {
    /** Transport type: "KAFKA" or "IN_MEMORY". */
    const val EVENTS_TYPE = "EVENTS_TYPE"
    /** Kafka bootstrap servers (e.g. localhost:9092). */
    const val KAFKA_BOOTSTRAP_SERVERS = "KAFKA_BOOTSTRAP_SERVERS"
    /** Consumer group id for the subscriber. */
    const val KAFKA_GROUP_ID = "KAFKA_GROUP_ID"
    /** Client id for Kafka producer/consumer. */
    const val KAFKA_CLIENT_ID = "KAFKA_CLIENT_ID"
}