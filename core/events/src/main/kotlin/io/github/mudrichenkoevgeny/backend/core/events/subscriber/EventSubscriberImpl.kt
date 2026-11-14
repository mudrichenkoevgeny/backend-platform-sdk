package io.github.mudrichenkoevgeny.backend.core.events.subscriber

import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.BackgroundScope
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.events.config.model.EventsConfig
import io.github.mudrichenkoevgeny.backend.core.events.event.AppEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [EventSubscriber] implementation backed by Apache Kafka.
 *
 * Launches a poll loop in the [CoroutineScope] qualified with [BackgroundScope], deserializes
 * messages to type `T` via [FoundationJson], extracts metadata from headers, and invokes the given handler. Deserialization or
 * handler errors are logged via [AppLogger]; the loop continues until the scope is cancelled. Uses `auto.offset.reset=earliest`
 * and a 1s poll timeout.
 */
@Singleton
public class EventSubscriberImpl @Inject constructor(
    private val eventsConfig: EventsConfig,
    private val appLogger: AppLogger,
    @param:BackgroundScope private val scope: CoroutineScope
) : EventSubscriber {
    private val json = FoundationJson

    override fun <T : AppEvent> subscribe(
        topic: String,
        type: Class<T>,
        handler: suspend (T, Map<String, String>) -> Unit
    ) {
        scope.launch {
            val props = Properties().apply {
                put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, eventsConfig.kafkaBootstrapServers)
                put(ConsumerConfig.GROUP_ID_CONFIG, eventsConfig.kafkaGroupId)
                put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
                put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OFFSET_RESET_EARLIEST)
            }

            KafkaConsumer<String, String>(props).use { consumer ->
                consumer.subscribe(listOf(topic))

                while (isActive) {
                    val records = consumer.poll(Duration.ofMillis(POLL_TIMEOUT_MS))

                    for (record in records) {
                        val event = try {
                            val serializer = json.serializersModule.serializer(type)

                            @Suppress("UNCHECKED_CAST")
                            json.decodeFromString(
                                serializer,
                                record.value()
                            ) as T
                        } catch (e: SerializationException) {
                            appLogger.logError(CommonError.Internal(e))
                            continue
                        } catch (e: IllegalArgumentException) {
                            appLogger.logError(CommonError.Internal(e))
                            continue
                        }

                        val metadata = record.headers().associate { it.key() to String(it.value()) }

                        try {
                            handler(event, metadata)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                            appLogger.logError(CommonError.Internal(e))
                        }
                    }
                }
            }
        }
    }

    public companion object {
        private const val OFFSET_RESET_EARLIEST = "earliest"
        private const val POLL_TIMEOUT_MS = 1000L
    }
}
