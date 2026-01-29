package io.github.mudrichenkoevgeny.backend.core.events.subscriber

import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.BackgroundScope
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.serialization.DefaultJson
import io.github.mudrichenkoevgeny.backend.core.events.config.model.EventsConfig
import io.github.mudrichenkoevgeny.backend.core.events.event.AppEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.serializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventSubscriberImpl @Inject constructor(
    private val eventsConfig: EventsConfig,
    private val appLogger: AppLogger,
    @param:BackgroundScope private val scope: CoroutineScope
) : EventSubscriber {

    private val json = DefaultJson

    @Suppress("UNCHECKED_CAST")
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
                        try {
                            val serializer = json.serializersModule.serializer(type)
                            val event = json.decodeFromString(serializer, record.value()) as T

                            val metadata = record.headers()
                                .associate { it.key() to String(it.value()) }

                            handler(event, metadata)
                        } catch (t: Throwable) {
                            appLogger.logError(CommonError.System(t))
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val OFFSET_RESET_EARLIEST = "earliest"
        private const val POLL_TIMEOUT_MS = 1000L
    }
}