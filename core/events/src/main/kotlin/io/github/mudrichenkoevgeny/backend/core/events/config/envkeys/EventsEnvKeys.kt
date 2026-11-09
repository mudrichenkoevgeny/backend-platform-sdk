package io.github.mudrichenkoevgeny.backend.core.events.config.envkeys

import io.github.mudrichenkoevgeny.backend.core.events.config.factory.EventsConfigFactory
import io.github.mudrichenkoevgeny.backend.core.events.config.model.EventsConfig

/**
 * Environment variable keys used to build [EventsConfig]. Values are read by [EventsConfigFactory] implementations.
 */
public object EventsEnvKeys {
    /** Transport type: "KAFKA" or "IN_MEMORY". */
    public const val EVENTS_TYPE: String = "EVENTS_TYPE"

    /** Kafka bootstrap servers (e.g. localhost:9092). */
    public const val KAFKA_BOOTSTRAP_SERVERS: String = "KAFKA_BOOTSTRAP_SERVERS"

    /** Consumer group id for the subscriber. */
    public const val KAFKA_GROUP_ID: String = "KAFKA_GROUP_ID"

    /** Client id for Kafka producer/consumer. */
    public const val KAFKA_CLIENT_ID: String = "KAFKA_CLIENT_ID"
}
