package io.github.mudrichenkoevgeny.backend.core.events.config.model

import io.github.mudrichenkoevgeny.backend.core.events.enums.EventsType

data class EventsConfig(
    val eventsType: EventsType,
    val kafkaBootstrapServers: String,
    val kafkaGroupId: String,
    val kafkaClientId: String
)