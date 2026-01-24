package com.mudrichenkoevgeny.backend.core.events.publisher

import com.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import com.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import com.mudrichenkoevgeny.backend.core.common.serialization.DefaultJson
import com.mudrichenkoevgeny.backend.core.events.config.model.EventsConfig
import com.mudrichenkoevgeny.backend.core.events.event.AppEvent
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.KSerializer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resumeWithException

@Singleton
class EventPublisherImpl @Inject constructor(
    private val eventsConfig: EventsConfig,
    private val appLogger: AppLogger
) : EventPublisher {

    private val json = DefaultJson

    private val producer: KafkaProducer<String, String> by lazy {
        val props = Properties().apply {
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, eventsConfig.kafkaBootstrapServers)
            put(ProducerConfig.CLIENT_ID_CONFIG, eventsConfig.kafkaClientId)
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            put(ProducerConfig.ACKS_CONFIG, "all")
            put(ProducerConfig.RETRIES_CONFIG, 3)
        }
        KafkaProducer<String, String>(props)
    }

    override suspend fun <T : AppEvent> publish(
        topic: String,
        event: T,
        serializer: KSerializer<T>,
        metadata: Map<String, String>
    ) {
        val jsonPayload = try {
            json.encodeToString(serializer, event)
        } catch (t: Throwable) {
            appLogger.logError(CommonError.System(t))
            throw t
        }

        val record = try {
            ProducerRecord<String, String>(topic, jsonPayload).apply {
                metadata.forEach { (k, v) -> headers().add(k, v.toByteArray()) }
            }
        } catch (t: Throwable) {
            appLogger.logError(CommonError.System(t))
            throw t
        }

        suspendCancellableCoroutine { continuation ->
            val future = producer.send(record) { _, exception ->
                if (exception != null) {
                    continuation.resumeWithException(exception)
                } else {
                    continuation.resumeWith(Result.success(Unit))
                }
            }

            continuation.invokeOnCancellation {
                future.cancel(true)
            }
        }
    }
}