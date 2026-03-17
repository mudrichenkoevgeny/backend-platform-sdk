package io.github.mudrichenkoevgeny.backend.core.events.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.events.config.envkeys.EventsEnvKeys
import io.github.mudrichenkoevgeny.backend.core.events.config.model.EventsConfig
import io.github.mudrichenkoevgeny.backend.core.events.model.EventsType
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EventsConfigFactoryImplTest {

    private companion object {
        private const val KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
        private const val KAFKA_GROUP_ID = "test-group"
        private const val KAFKA_CLIENT_ID = "test-client"
    }

    private val envReader = mockk<EnvReader>()

    @Test
    fun `create builds EventsConfig from env`() {
        every { envReader.getByKey(EventsEnvKeys.EVENTS_TYPE) } returns "IN_MEMORY"
        every { envReader.getByKey(EventsEnvKeys.KAFKA_BOOTSTRAP_SERVERS) } returns KAFKA_BOOTSTRAP_SERVERS
        every { envReader.getByKey(EventsEnvKeys.KAFKA_GROUP_ID) } returns KAFKA_GROUP_ID
        every { envReader.getByKey(EventsEnvKeys.KAFKA_CLIENT_ID) } returns KAFKA_CLIENT_ID

        val factory = EventsConfigFactoryImpl(envReader)

        val config: EventsConfig = factory.create()

        assertEquals(EventsType.IN_MEMORY, config.eventsType)
        assertEquals(KAFKA_BOOTSTRAP_SERVERS, config.kafkaBootstrapServers)
        assertEquals(KAFKA_GROUP_ID, config.kafkaGroupId)
        assertEquals(KAFKA_CLIENT_ID, config.kafkaClientId)
    }

    @Test
    fun `create falls back to KAFKA when EVENTS_TYPE is invalid`() {
        every { envReader.getByKey(EventsEnvKeys.EVENTS_TYPE) } returns "UNKNOWN"
        every { envReader.getByKey(EventsEnvKeys.KAFKA_BOOTSTRAP_SERVERS) } returns KAFKA_BOOTSTRAP_SERVERS
        every { envReader.getByKey(EventsEnvKeys.KAFKA_GROUP_ID) } returns KAFKA_GROUP_ID
        every { envReader.getByKey(EventsEnvKeys.KAFKA_CLIENT_ID) } returns KAFKA_CLIENT_ID

        val factory = EventsConfigFactoryImpl(envReader)

        val config: EventsConfig = factory.create()

        assertEquals(EventsType.KAFKA, config.eventsType)
    }
}

