# core/events

Event publishing and subscribing for SDK-based applications (Kafka by default).

## What it provides

- **Config**: [EventsConfig] built by [EventsConfigFactory] from env via [EventsEnvKeys].
- **Publisher**: [EventPublisher] to publish [AppEvent] instances to a topic (Kafka implementation: [EventPublisherImpl]).
- **Subscriber**: [EventSubscriber] to subscribe to a topic and handle deserialized [AppEvent] instances (Kafka implementation: [EventSubscriberImpl]).
- **DI wiring**: [EventsModules] aggregates config + publisher + subscriber bindings.

## Environment variables

The following env keys are used by [EventsConfigFactoryImpl]:

- `EVENTS_TYPE` — `"KAFKA"` or `"IN_MEMORY"` (fallback: `"KAFKA"`).
- `KAFKA_BOOTSTRAP_SERVERS` — e.g. `localhost:9092`.
- `KAFKA_GROUP_ID` — consumer group id.
- `KAFKA_CLIENT_ID` — client id for producer/consumer.

See: [EventsEnvKeys].

## Usage

- Add dependency on `core:events`. Depends on `core:common`.
- Install [EventsModules] in your Dagger component.
- Read configuration from env (via [EventsConfigModule]) and inject [EventPublisher]/[EventSubscriber] where needed.

### Publish an event

```kotlin
@Serializable
data class UserCreatedEvent(val userId: String) : AppEvent

suspend fun publishExample(publisher: EventPublisher) {
    publisher.publish(
        topic = "user.created",
        event = UserCreatedEvent(userId = "123"),
        serializer = UserCreatedEvent.serializer(),
        metadata = mapOf("traceId" to "abc")
    )
}
```

### Subscribe to a topic

```kotlin
@Serializable
data class UserCreatedEvent(val userId: String) : AppEvent

fun subscribeExample(subscriber: EventSubscriber) {
    subscriber.subscribe(
        topic = "user.created",
        type = UserCreatedEvent::class.java
    ) { event, metadata ->
        // handle event + metadata (Kafka headers)
    }
}
```

[AppEvent]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/events/event/AppEvent.kt
[EventsConfig]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/events/config/model/EventsConfig.kt
[EventsEnvKeys]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/events/config/envkeys/EventsEnvKeys.kt
[EventsConfigFactory]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/events/config/factory/EventsConfigFactory.kt
[EventsConfigFactoryImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/events/config/factory/EventsConfigFactoryImpl.kt
[EventPublisher]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/events/publisher/EventPublisher.kt
[EventPublisherImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/events/publisher/EventPublisherImpl.kt
[EventSubscriber]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/events/subscriber/EventSubscriber.kt
[EventSubscriberImpl]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/events/subscriber/EventSubscriberImpl.kt
[EventsModules]: src/main/kotlin/io/github/mudrichenkoevgeny/backend/core/events/di/EventsModules.kt

