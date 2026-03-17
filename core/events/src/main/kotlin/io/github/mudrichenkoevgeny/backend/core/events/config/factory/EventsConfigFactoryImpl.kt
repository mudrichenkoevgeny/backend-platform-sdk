package io.github.mudrichenkoevgeny.backend.core.events.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.events.config.envkeys.EventsEnvKeys
import io.github.mudrichenkoevgeny.backend.core.events.config.model.EventsConfig
import io.github.mudrichenkoevgeny.backend.core.events.model.EventsType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [EventsConfigFactory] implementation that reads [EventsConfig] from environment variables via [EnvReader],
 * using [EventsEnvKeys] for key names. [EventsType.fromString] is used to parse the transport type.
 */
@Singleton
class EventsConfigFactoryImpl @Inject constructor(
    private val envReader: EnvReader
) : EventsConfigFactory {

    override fun create(): EventsConfig {
        val eventsType = EventsType.fromString(envReader.getByKey(EventsEnvKeys.EVENTS_TYPE))
        val kafkaBootstrapServers = envReader.getByKey(EventsEnvKeys.KAFKA_BOOTSTRAP_SERVERS)
        val kafkaGroupId = envReader.getByKey(EventsEnvKeys.KAFKA_GROUP_ID)
        val kafkaClientId = envReader.getByKey(EventsEnvKeys.KAFKA_CLIENT_ID)

        return EventsConfig(
            eventsType = eventsType,
            kafkaBootstrapServers = kafkaBootstrapServers,
            kafkaGroupId = kafkaGroupId,
            kafkaClientId = kafkaClientId
        )
    }
}