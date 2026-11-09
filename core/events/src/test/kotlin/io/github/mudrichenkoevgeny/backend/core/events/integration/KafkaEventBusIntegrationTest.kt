package io.github.mudrichenkoevgeny.backend.core.events.integration

import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.testing.docker.RequiresDocker
import io.github.mudrichenkoevgeny.backend.core.events.config.model.EventsConfig
import io.github.mudrichenkoevgeny.backend.core.events.event.AppEvent
import io.github.mudrichenkoevgeny.backend.core.events.model.EventsType
import io.github.mudrichenkoevgeny.backend.core.events.publisher.EventPublisherImpl
import io.github.mudrichenkoevgeny.backend.core.events.subscriber.EventSubscriberImpl
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import kotlin.time.Duration.Companion.seconds

@RequiresDocker
internal class KafkaEventBusIntegrationTest {
    @Serializable
    private data class TestDomainEvent(
        val id: String,
        val payload: String
    ) : AppEvent

    @Test
    fun `kafka event bus should publish and consume domain events successfully`() {
        KafkaContainer(DockerImageName.parse(KAFKA_IMAGE)).use { kafka ->
            kafka.start()
            assertTrue(kafka.isRunning)

            val eventsConfig = EventsConfig(
                eventsType = EventsType.KAFKA,
                kafkaBootstrapServers = kafka.bootstrapServers,
                kafkaGroupId = KAFKA_GROUP_ID,
                kafkaClientId = KAFKA_CLIENT_ID
            )
            val appLogger = mockk<AppLogger>(relaxed = true)
            val subscriberScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

            val publisher = EventPublisherImpl(
                eventsConfig = eventsConfig,
                appLogger = appLogger
            )
            val subscriber = EventSubscriberImpl(
                eventsConfig = eventsConfig,
                appLogger = appLogger,
                scope = subscriberScope
            )

            try {
                val deferred = CompletableDeferred<Pair<TestDomainEvent, Map<String, String>>>()

                subscriber.subscribe(
                    topic = TEST_TOPIC,
                    type = TestDomainEvent::class.java
                ) { event, metadata ->
                    deferred.complete(event to metadata)
                }

                val testEvent = TestDomainEvent(
                    id = "evt-1001",
                    payload = "Integration Test Event Payload"
                )
                val testMetadata = mapOf(METADATA_KEY to METADATA_VALUE)

                runBlocking {
                    publisher.publish(
                        topic = TEST_TOPIC,
                        event = testEvent,
                        serializer = TestDomainEvent.serializer(),
                        metadata = testMetadata
                    )

                    val (receivedEvent, receivedMetadata) = withTimeout(TIMEOUT_SECONDS.seconds) {
                        deferred.await()
                    }

                    assertEquals(testEvent, receivedEvent)
                    assertEquals(METADATA_VALUE, receivedMetadata[METADATA_KEY])
                }
            } finally {
                subscriberScope.cancel()
            }
        }
    }

    private companion object {
        private const val KAFKA_IMAGE = "apache/kafka-native:3.8.0"
        private const val TEST_TOPIC = "test.integration.events"
        private const val KAFKA_GROUP_ID = "test-group"
        private const val KAFKA_CLIENT_ID = "test-client"
        private const val METADATA_KEY = "correlation-id"
        private const val METADATA_VALUE = "test-12345"
        private const val TIMEOUT_SECONDS = 30L
    }
}
