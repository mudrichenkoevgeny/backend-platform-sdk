package io.github.mudrichenkoevgeny.backend.core.events.publisher

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.events.config.model.EventsConfig
import io.github.mudrichenkoevgeny.backend.core.events.event.AppEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
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

/**
 * [EventPublisher] implementation backed by Apache Kafka.
 *
 * Serializes events to JSON via [FoundationJson] and the provided [KSerializer], sends the record to Kafka with optional
 * metadata in headers. Serialization and send errors are logged via [AppLogger] and rethrown to the caller. Sending is done
 * in a suspend function via [suspendCancellableCoroutine] with cancellation support.
 */
@Singleton
class EventPublisherImpl @Inject constructor(
    private val eventsConfig: EventsConfig,
    private val appLogger: AppLogger
) : EventPublisher {

    private val json = FoundationJson

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
            appLogger.logError(CommonError.Internal(t))
            throw t
        }

        val record = try {
            ProducerRecord<String, String>(topic, jsonPayload).apply {
                metadata.forEach { (k, v) -> headers().add(k, v.toByteArray()) }
            }
        } catch (t: Throwable) {
            appLogger.logError(CommonError.Internal(t))
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