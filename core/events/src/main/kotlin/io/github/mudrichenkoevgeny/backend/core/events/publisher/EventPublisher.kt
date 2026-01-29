package io.github.mudrichenkoevgeny.backend.core.events.publisher

import io.github.mudrichenkoevgeny.backend.core.events.event.AppEvent
import kotlinx.serialization.KSerializer

interface EventPublisher {
    suspend fun <T : AppEvent> publish(
        topic: String,
        event: T,
        serializer: KSerializer<T>,
        metadata: Map<String, String> = emptyMap()
    )
}