package io.github.mudrichenkoevgeny.backend.core.events.model

/**
 * Event transport type used by the events module.
 *
 * Selects which publisher/subscriber implementation should be used by the host application.
 */
public enum class EventsType {
    /** Apache Kafka-based transport. */
    KAFKA,

    /** In-memory transport (useful for local development or tests). */
    IN_MEMORY

    ;

    public companion object {
        /**
         * Parses a string into [EventsType].
         *
         * @param value environment variable value (e.g. "KAFKA", "IN_MEMORY"); may be `null` or blank.
         * @return the corresponding [EventsType]; returns [KAFKA] when value is `null`, blank, or unrecognized.
         */
        public fun fromString(value: String?): EventsType = try {
            value?.uppercase()?.let { valueOf(it) } ?: KAFKA
        } catch (_: IllegalArgumentException) {
            KAFKA
        }
    }
}
