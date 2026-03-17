package io.github.mudrichenkoevgeny.backend.core.events.publisher

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.events.config.model.EventsConfig
import io.github.mudrichenkoevgeny.backend.core.events.event.AppEvent
import io.github.mudrichenkoevgeny.backend.core.events.model.EventsType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EventPublisherImplTest {

    private data class TestEvent(val value: String) : AppEvent

    private class ThrowingSerializer<T : Any>(
        private val throwable: Throwable
    ) : KSerializer<T> {

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ThrowingSerializer")

        override fun serialize(encoder: Encoder, value: T) {
            throw throwable
        }

        override fun deserialize(decoder: Decoder): T {
            throw UnsupportedOperationException("deserialize is not used in this test")
        }
    }

    @Test
    fun `publish logs and rethrows when serialization fails`() {
        val throwable = IllegalStateException("boom")
        val appLogger = mockk<AppLogger>(relaxed = true)
        val eventsConfig = EventsConfig(
            eventsType = EventsType.KAFKA,
            kafkaBootstrapServers = "localhost:9092",
            kafkaGroupId = "group",
            kafkaClientId = "client"
        )

        val publisher = EventPublisherImpl(
            eventsConfig = eventsConfig,
            appLogger = appLogger
        )

        val loggedError = slot<AppError>()
        every { appLogger.logError(capture(loggedError)) } returns Unit

        val thrown = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                @Suppress("UNCHECKED_CAST")
                publisher.publish(
                    topic = "topic",
                    event = TestEvent("x"),
                    serializer = ThrowingSerializer<TestEvent>(throwable) as KSerializer<TestEvent>,
                    metadata = emptyMap()
                )
            }
        }

        assertSame(throwable, thrown)
        assertTrue(loggedError.captured is CommonError.Internal)
        assertSame(throwable, (loggedError.captured as CommonError.Internal).throwable)
        verify(exactly = 1) { appLogger.logError(any()) }
    }
}

